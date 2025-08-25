require("dotenv").config();

const express = require("express");
const fs = require("fs");
const path = require("path");
const { google } = require("googleapis");
const mysql = require("mysql2/promise");
const csv = require("csv-parser");
const moment = require("moment");
const multer = require("multer");
const upload = multer({ dest: "uploads/" });
const cors = require("cors");
const crypto = require("crypto");
const jwt = require("jsonwebtoken");
const axios = require("axios");

const app = express();
const port = process.env.PORT || 10000;

app.use(cors());
app.use(express.json());
app.set("json spaces", 2);

const JWT_SECRET_KEY = process.env.JWT_SECRET_KEY;
const AZURE_API_KEY = process.env.AZURE_API_KEY;
const AZURE_ENDPOINT = process.env.AZURE_ENDPOINT;
const DEPLOYMENT_NAME = process.env.DEPLOYMENT_NAME;
const API_VERSION = process.env.API_VERSION;

const db = mysql.createPool({
  host: process.env.MYSQL_HOST || "shuttle.proxy.rlwy.net",
  port: Number(process.env.MYSQL_PORT) || 50760,
  user: process.env.MYSQL_USER || "root",
  password: process.env.MYSQL_PASSWORD || "YFrWuiOvwgOAhNYzoyRbCNIenrOiWckT",
  database: process.env.MYSQL_DATABASE || "railway",
  timezone: process.env.DB_TIMEZONE || "+08:00",
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
});

const exercisePool = db;
const healthPool = db;
const pool = db;
module.exports = pool;

require("dotenv").config();

(async () => {
  try {
    // 確認連線是否成功，可以用 getConnection() 試連一次
    const connection = await db.getConnection();
    console.log("資料庫連接成功");
    connection.release(); // 釋放連線回池中
  } catch (err) {
    console.error("資料庫連接失敗:", err.message);
  }
})();

// 範例：建立一個 async 函式用來取得連線（通常直接用 pool 就可以了，不一定要自己寫 getPool）
async function getPool() {
  return db;
}

// 密碼雜湊
function hashPw(pw) {
  return crypto.createHash("sha256").update(pw, "utf-8").digest("hex");
}

// JWT 生成
function genToken(username) {
  return jwt.sign({ username }, JWT_SECRET_KEY, { expiresIn: "2h" });
}

// 驗證 Token middleware
function tokenRequired(req, res, next) {
  const token = req.query.token || "";
  try {
    const decoded = jwt.verify(token, JWT_SECRET_KEY);
    req.user = decoded.username;
    next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
}

// 插入資料函式 (改成 async/await)
async function insertIntoDatabase(rows, userId) {
  const valuesToInsert = [];

  for (const row of rows) {
    // 轉換日期格式，取日期部分（yyyy-mm-dd）

    const originalDate = row["測量日期"];

    const date = new Date(originalDate);
    date.setHours(date.getHours() + 0); // 加8小時

    const formattedDate = date.toISOString().split("T")[0];

    // 檢查資料是否已存在（以 measure_at, systolic_mmHg, diastolic_mmHg, pulse_bpm, user_id 唯一判斷）
    const [results] = await db.execute(
      `
      SELECT COUNT(*) AS count
      FROM BloodPressure
      WHERE measure_at = ?
        AND systolic_mmHg = ?
        AND diastolic_mmHg = ?
        AND pulse_bpm = ?
        AND user_id = ?
      `,
      [
        formattedDate,
        row["收縮壓(mmHg)"],
        row["舒張壓(mmHg)"],
        row["脈搏(bpm)"],
        userId,
      ]
    );

    console.log(
      `查詢結果 for ${formattedDate} - ${row["收縮壓(mmHg)"]}, ${row["舒張壓(mmHg)"]}, ${row["脈搏(bpm)"]}, user_id=${userId}: `,
      results[0].count
    );

    if (results[0].count === 0) {
      valuesToInsert.push([
        formattedDate || null,
        row["時區"] || null,
        row["收縮壓(mmHg)"] || null,
        row["舒張壓(mmHg)"] || null,
        row["脈搏(bpm)"] || null,
        row["檢測到不規則脈搏"] || null,
        row["不規則脈搏次數(次數)"] || null,
        row["身體晃動檢測"] || null,
        row["壓脈帶緊度檢查"] || null,
        row["測量姿勢正確符號"] || null,
        row["室温(°C)"] || null,
        row["測試模式"] || null,
        row["型號"] || null,
        userId, // 新增 user_id 欄位
      ]);
    } else {
      console.log("資料已存在，跳過插入");
    }
  }

  if (valuesToInsert.length > 0) {
    const insertQuery = `
      INSERT INTO BloodPressure
      (measure_at, timezone, systolic_mmHg, diastolic_mmHg, pulse_bpm, irregular_pulse,
       irregular_count, motion_detected, cuff_tightness_ok, posture_ok, room_temp_c,
       test_mode, device_model, user_id)
      VALUES ?
    `;

    const [result] = await db.query(insertQuery, [valuesToInsert]);
    console.log(`成功插入 ${result.affectedRows} 筆資料`);
    return result;
  } else {
    console.log("沒有需要插入的新資料");
    return;
  }
}

app.get("/getBloodPressureByValue", async (req, res) => {
  const { type, min, max, username } = req.query;

  // 參數基本檢查
  if (!type || isNaN(min) || isNaN(max) || !username) {
    return res
      .status(400)
      .json({ error: "請提供正確的 type、min、max 和 username 參數" });
  }

  // 對應新資料表欄位名稱
  const columns = {
    systolic: "`systolic_mmHg`",
    diastolic: "`diastolic_mmHg`",
  };

  if (!columns[type]) {
    return res
      .status(400)
      .json({ error: "無效的 type 參數，僅支持 'systolic' 或 'diastolic'" });
  }

  try {
    // 先取得 user_id
    const [userRows] = await db.execute(
      "SELECT user_id FROM Users WHERE username = ?",
      [username]
    );

    if (userRows.length === 0) {
      return res.status(404).json({ error: "找不到該使用者" });
    }

    const user_id = userRows[0].user_id;
    const column = columns[type];
    const minValue = parseInt(min, 10);
    const maxValue = parseInt(max, 10);

    // 查詢指定使用者且血壓在區間內的資料
    const query = `
      SELECT measure_at, systolic_mmHg, diastolic_mmHg
      FROM BloodPressure
      WHERE user_id = ? AND ${column} BETWEEN ? AND ?
    `;

    const [results] = await db.execute(query, [user_id, minValue, maxValue]);
    res.json(results);
  } catch (err) {
    console.error("查詢失敗:", err.message);
    res.status(500).json({ error: "資料庫錯誤" });
  }
});

app.get("/getFilteredBloodPressureData", async (req, res) => {
  const { startDate, endDate, filter, username } = req.query;

  // 基本參數檢查
  if (!startDate || !endDate || !username) {
    return res
      .status(400)
      .json({ error: "請提供有效的 startDate、endDate 和 username" });
  }

  try {
    // 先從 Users 表取得 user_id
    const [userRows] = await db.execute(
      "SELECT user_id FROM Users WHERE username = ?",
      [username]
    );

    if (userRows.length === 0) {
      return res.status(404).json({ error: "找不到該使用者" });
    }

    const user_id = userRows[0].user_id;

    // SQL 查詢語句
    let query = `
      SELECT measure_at, systolic_mmHg, diastolic_mmHg
      FROM BloodPressure
      WHERE measure_at BETWEEN ? AND ? AND user_id = ?`;

    const queryParams = [startDate, endDate, user_id];

    console.log("收到的查詢參數:", req.query);

    // 根據 filter 加入條件
    if (filter && filter !== "all") {
      switch (filter) {
        case "normal":
          query += `
            AND systolic_mmHg BETWEEN ? AND ?
            AND diastolic_mmHg BETWEEN ? AND ?`;
          queryParams.push(90, 120, 60, 80);
          break;

        case "elevated":
          query += `
            AND systolic_mmHg <= 140
            AND diastolic_mmHg <= 90
            AND (systolic_mmHg BETWEEN 121 AND 140 OR diastolic_mmHg BETWEEN 81 AND 90)`;
          break;

        case "low":
          query += `
            AND (systolic_mmHg < ? OR diastolic_mmHg < ?)`;
          queryParams.push(90, 60);
          break;

        case "danger":
          query += `
            AND (systolic_mmHg > ? OR diastolic_mmHg > ?)`;
          queryParams.push(140, 90);
          break;

        default:
          return res.status(400).json({ error: "無效的 filter 參數" });
      }
    }

    console.log("SQL 查詢:", query);
    console.log("參數:", queryParams);

    const [results] = await db.execute(query, queryParams);
    res.json(results);
  } catch (err) {
    console.error("查詢失敗:", err.message);
    res.status(500).json({ error: "資料庫錯誤" });
  }
});

// 路由：根据日期范围查询体重数据
app.get("/getFilteredWeightData", async (req, res) => {
  const { startDate, endDate, user_id } = req.query;

  if (!startDate || !endDate || !user_id || isNaN(user_id)) {
    return res
      .status(400)
      .json({ error: "請提供有效的 startDate、endDate 和 user_id" });
  }

  const query = `
    SELECT measure_at, weight_kg
    FROM WeightData
    WHERE measure_at BETWEEN ? AND ? AND user_id = ?
  `;

  const queryParams = [startDate, endDate, parseInt(user_id)];

  console.log("收到的查詢參數:", req.query);

  try {
    const [results] = await db.execute(query, queryParams);
    res.json(results);
  } catch (err) {
    console.error("查詢失敗:", err.message);
    res.status(500).json({ error: "資料庫錯誤" });
  }
});

app.use(express.json());

app.post("/submit-anxiety-score", async (req, res) => {
  const { username, measurementDate, score, suggestion } = req.body;

  console.log(req.body); // 檢查請求體內容

  if (!username || !measurementDate || !score || !suggestion) {
    return res.status(400).json({ message: "Missing required fields" });
  }

  try {
    const [rows] = await db.execute(
      "SELECT user_id FROM Users WHERE username = ?",
      [username]
    );

    if (rows.length === 0) {
      return res.status(404).json({ message: "User not found" });
    }

    const user_id = rows[0].user_id; // ← 這裡改成 user_id

    const insertQuery = `
      INSERT INTO AnxietyIndex (user_id, measure_at, score, suggestion)
      VALUES (?, ?, ?, ?)
    `;

    await db.execute(insertQuery, [
      user_id,
      measurementDate,
      score,
      suggestion,
    ]);

    res.status(200).json({ message: "Data saved successfully" });
  } catch (err) {
    console.error("插入失敗:", err.message);
    res.status(500).json({ message: "Error storing data", error: err.message });
  }
});

app.get("/get-anxiety-scores", async (req, res) => {
  const { startDate, endDate, username } = req.query;

  if (!startDate || !endDate || !username) {
    return res.status(400).json({ error: "請提供 username、起始與結束日期" });
  }

  // 驗證日期格式
  if (
    !moment(startDate, "YYYY-MM-DD", true).isValid() ||
    !moment(endDate, "YYYY-MM-DD", true).isValid()
  ) {
    return res.status(400).json({ error: "日期格式應為 YYYY-MM-DD" });
  }

  try {
    // 取得對應 user_id
    const [userRows] = await db.execute(
      "SELECT user_id FROM Users WHERE username = ?",
      [username]
    );

    if (userRows.length === 0) {
      return res.status(404).json({ error: "找不到該使用者" });
    }

    const user_id = userRows[0].user_id; // ← 這裡改成 user_id

    // 查詢該 user_id 的資料
    const query = `
      SELECT measure_at AS measurementDate, score
      FROM AnxietyIndex
      WHERE user_id = ? AND measure_at BETWEEN ? AND ?
      ORDER BY measure_at ASC
    `;

    const [results] = await db.execute(query, [user_id, startDate, endDate]);
    res.json(results);
  } catch (err) {
    console.error("資料查詢失敗", err.message);
    res.status(500).json({ error: "資料庫錯誤" });
  }
});

app.post("/upload", upload.single("csvFile"), async (req, res) => {
  console.log("收到上傳請求");

  if (!req.file) {
    console.error("錯誤: 沒有收到 CSV 檔案");
    return res.status(400).json({ error: "請上傳 CSV 檔案" });
  }

  const filePath = req.file.path;
  console.log("CSV 檔案已上傳，存放路徑:", filePath);

  const rows = [];

  const parseCsv = () =>
    new Promise((resolve, reject) => {
      fs.createReadStream(filePath)
        .pipe(csv({ mapHeaders: ({ header }) => header.trim() }))
        .on("data", (row) => {
          const fixedRow = {};
          Object.keys(row).forEach((key) => {
            const cleanKey = key.replace(/^"|"$/g, "").trim();
            fixedRow[cleanKey] = row[key];
          });
          console.log("修正後的行數據:", fixedRow);
          rows.push(fixedRow);
        })
        .on("end", () => {
          console.log("CSV 解析完成，共", rows.length, "行數據");
          resolve();
        })
        .on("error", (error) => {
          reject(error);
        });
    });

  try {
    await parseCsv();

    const { username } = req.body; // 改用 username

    if (!username) {
      return res.status(400).json({ error: "缺少 username" });
    }

    // 先從 Users 表查 user_id
    const [userRows] = await db.execute(
      "SELECT user_id FROM Users WHERE username = ?",
      [username]
    );

    if (userRows.length === 0) {
      return res.status(404).json({ error: "找不到該使用者" });
    }

    const userId = userRows[0].user_id;

    // 再用 userId 插入資料
    await insertIntoDatabase(rows, userId);

    res.json({ message: "CSV 上傳並儲存成功！" });
  } catch (error) {
    console.error("錯誤:", error);
    res.status(500).json({ error: "處理失敗" });
  } finally {
    fs.unlink(filePath, (err) => {
      if (err) console.error("刪除檔案失敗:", err);
      else console.log("暫存檔案已刪除:", filePath);
    });
  }
});

// 1️⃣ 透過 username 取得 user_id
app.get("/get_user_id", async (req, res) => {
  const { username } = req.query;
  if (!username) return res.status(400).json({ error: "缺少 username" });

  try {
    const [rows] = await pool.query(
      "SELECT user_id FROM Users WHERE username = ?",
      [username]
    );
    if (rows.length === 0)
      return res.status(404).json({ error: "使用者不存在" });

    res.json(rows[0].user_id); // 回傳 user_id
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "伺服器錯誤" });
  }
});

// 2️⃣ 取得指定日期範圍血壓資料
// 取得指定日期範圍血壓資料 (轉換成台灣時間)
// 取得指定日期範圍血壓資料
app.get("/get_blood_pressure_range", async (req, res) => {
  const { userId, startDate, endDate } = req.query;
  if (!userId || !startDate || !endDate)
    return res.status(400).json({ error: "缺少參數" });

  try {
    const [rows] = await pool.query(
      `SELECT id, user_id,
              systolic_mmHg AS systolic,
              diastolic_mmHg AS diastolic,
              pulse_bpm AS pulse,
              CONVERT_TZ(measure_at, '+00:00', '+08:00') AS measure_at
       FROM BloodPressure
       WHERE user_id = ? AND measure_at BETWEEN ? AND ?
       ORDER BY measure_at ASC`,
      [userId, startDate, endDate]
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "伺服器錯誤" });
  }
});

// 刪除多筆血壓資料
app.post("/delete_blood_pressure_multiple", async (req, res) => {
  const { userId, ids } = req.body;
  if (!userId || !ids || !Array.isArray(ids) || ids.length === 0)
    return res.status(400).json({ error: "缺少參數" });

  try {
    const placeholders = ids.map(() => "?").join(",");
    const sql = `DELETE FROM BloodPressure WHERE user_id = ? AND id IN (${placeholders})`;
    await pool.query(sql, [userId, ...ids]);
    res.json({ message: "刪除成功" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "伺服器錯誤" });
  }
});

//-----------------------------------------(建一)-----------------------------------------------------

async function analyzeWithGPT(username, summaryText) {
  const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;

  // Debug log
  console.log(`📤 GPT 輸入 (${username}):\n${summaryText}`);

  const messages = [
    {
      role: "system",
      content: "你是一位健康分析小幫手，專門協助中老年人做健康建議。",
    },
    {
      role: "user",
      content: `
請分析以下使用者「${username}」的健康摘要資料，並以【條列式】＋【加入 Emoji】的方式回答下列問題。
⚠️ 請注意：
- 不要使用 Markdown 語法（如 ###、**、- 等符號）
- 回覆請保持簡潔、換行分段、乾淨清楚

🩺 1. 是否健康異常？
請簡要說明健康狀態，若有異常，請列出異常類型與數值（如高血壓、低脈搏）

📈 2. 是否有趨勢變化？
如：近期血壓上升、體重逐步下降等，請簡明扼要說明

💡 3. 給出健康建議（適合中老年人）
每類建議最多 2 點，請精簡扼要。可包含：
- 血壓建議
- 體重管理
- 飲食與運動
- 睡眠習慣等

⚠️ 4. 注意事項
若包含焦慮或血壓、體重同時異常，請提醒注意

請使用【繁體中文】，語氣溫和親切，容易理解。

健康摘要資料如下：
${summaryText}`.trim(),
    },
  ];

  try {
    const response = await axios.post(
      url,
      {
        messages,
        temperature: 0.7,
        max_tokens: 1000,
      },
      {
        headers: {
          "Content-Type": "application/json",
          "api-key": AZURE_API_KEY,
        },
        timeout: 20000,
      }
    );

    if (!response.data.choices || response.data.choices.length === 0) {
      console.warn("⚠️ GPT 回傳空白");
      return "⚠️ GPT 無回應，請稍後再試。";
    }

    const reply = response.data.choices[0].message?.content?.trim();
    return reply || "⚠️ GPT 回傳內容為空白。";
  } catch (err) {
    console.error("❌ GPT 分析失敗：", err.response?.data || err.message);
    return "❌ GPT 分析失敗，請稍後再試。";
  }
}

function summarizeBPRecords(rows) {
  return rows
    .map((row) => {
      const date = new Date(row.measure_at).toISOString().split("T")[0];
      return `【${date}】血壓：${row.systolic_mmHg}/${row.diastolic_mmHg}，脈搏：${row.pulse_bpm}`;
    })
    .join("\n");
}

function summarizeWeightRecords(rows) {
  return rows
    .map((row) => {
      const date = new Date(row.measured_at).toISOString().split("T")[0];
      const bmi = (row.weight / (row.height / 100) ** 2).toFixed(1);
      return `【${date}】體重：${row.weight} 公斤，BMI：${bmi}`;
    })
    .join("\n");
}

// 切分陣列工具（每 chunkSize 筆為一組）
function splitIntoChunks(arr, chunkSize) {
  return Array.from({ length: Math.ceil(arr.length / chunkSize) }, (_, i) =>
    arr.slice(i * chunkSize, i * chunkSize + chunkSize)
  );
}

// ──────────── API Routes ────────────

// 運動建議
app.get("/get_exercises", async (req, res) => {
  const condParam = req.query.conditions;
  if (!condParam) return res.json({ exercise: "請選擇至少一項病症。" });

  const conditions = condParam
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
    .sort();
  if (!conditions.length) return res.json({ exercise: "請選擇至少一項病症。" });

  const key = conditions.join(",");
  try {
    // 精準配對
    const [exact] = await exercisePool.query(
      `SELECT exercise, source_url 
       FROM exercise_suggestions 
       WHERE condition_combination = ?`,
      [key]
    );
    if (exact.length) {
      return res.json({
        exercise: exact[0].exercise,
        url: exact[0].source_url,
      });
    }

    // 部分配對
    const whereSql = conditions
      .map((c) => `FIND_IN_SET(?, condition_combination)`)
      .join(" AND ");
    const [partial] = await exercisePool.query(
      `SELECT exercise, source_url 
       FROM exercise_suggestions 
       WHERE ${whereSql}
       ORDER BY (LENGTH(condition_combination) - LENGTH(REPLACE(condition_combination, ',', '')) + 1) ASC
       LIMIT 1`,
      conditions
    );
    if (partial.length) {
      return res.json({
        exercise: partial[0].exercise,
        url: partial[0].source_url,
      });
    }

    return res.json({ exercise: "無相關建議，請諮詢專業人士。" });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: "資料庫查詢失敗" });
  }
});

// 單日血壓分析
app.get("/analyzeSingleBP", async (req, res) => {
  const { username, date } = req.query;
  if (!username || !date)
    return res.status(400).json({ error: "請提供 username 與 date" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender, u.height, u.weight,
              bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm,
              CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at
       FROM BloodPressure bp
       JOIN Users u ON u.user_id = bp.user_id
       WHERE u.username = ? AND DATE(CONVERT_TZ(bp.measure_at, '+00:00', '+08:00')) = ?
       ORDER BY bp.measure_at ASC`,
      [username, date]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "查無該日血壓資料" });

    const displayName = rows[0]?.display_name || username;
    const summary = summarizeBPRecords(rows);
    const gptResult = await analyzeWithGPT(displayName, summary);
    res.json({ analysis: gptResult });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "內部錯誤" });
  }
});

// 區間血壓分析
app.get("/analyzeRangeBP", async (req, res) => {
  const { username, start, end } = req.query;
  if (!username || !start || !end)
    return res
      .status(400)
      .json({ error: "請提供 username、start 與 end 日期" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender, u.height, u.weight,
              bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm,
              CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at
       FROM BloodPressure bp
       JOIN Users u ON u.user_id = bp.user_id
       WHERE u.username = ? AND bp.measure_at BETWEEN ? AND ?
       ORDER BY bp.measure_at ASC`,
      [username, start, end]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "該區間無血壓資料" });

    const displayName = rows[0]?.display_name || username;
    const chunks = splitIntoChunks(rows, 7);
    const results = [];

    for (const chunk of chunks) {
      const summary = summarizeBPRecords(chunk);
      const result = await analyzeWithGPT(displayName, summary);
      results.push(result);
    }

    res.json({ analysis: results.join("\n\n") });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "內部錯誤" });
  }
});

// 單日體重分析
app.get("/analyzeSingleWeight", async (req, res) => {
  const { username, date } = req.query;
  if (!username || !date)
    return res.status(400).json({ error: "請提供 username 與 date" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender,
              w.weight, w.height,
              CONVERT_TZ(w.measured_at, '+00:00', '+08:00') AS measured_at
       FROM weight_records w
       JOIN Users u ON u.username = w.username
       WHERE u.username = ? AND DATE(CONVERT_TZ(w.measured_at, '+00:00', '+08:00')) = ?
       ORDER BY w.measured_at ASC`,
      [username, date]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "查無該日體重資料" });

    const summary = summarizeWeightRecords(rows);
    const gptResult = await analyzeWithGPT(displayName, summary);
    const displayName = rows[0]?.display_name || username;
    res.json({ analysis: gptResult });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "內部錯誤" });
  }
});

// 區間體重分析
app.get("/analyzeRangeWeight", async (req, res) => {
  const { username, start, end } = req.query;
  if (!username || !start || !end)
    return res
      .status(400)
      .json({ error: "請提供 username、start 與 end 日期" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender,
              w.weight, w.height,
              CONVERT_TZ(w.measured_at, '+00:00', '+08:00') AS measured_at
       FROM weight_records w
       JOIN Users u ON u.username = w.username
       WHERE u.username = ? AND w.measured_at BETWEEN ? AND ?
       ORDER BY w.measured_at ASC`,
      [username, start, end]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "該區間無體重資料" });

    const displayName = rows[0]?.display_name || username;
    const chunks = splitIntoChunks(rows, 7);
    const results = [];

    for (const chunk of chunks) {
      const summary = summarizeWeightRecords(chunk);
      const result = await analyzeWithGPT(displayName, summary);
      results.push(result);
    }

    res.json({ analysis: results.join("\n\n") });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "內部錯誤" });
  }
});

app.get("/get_combined_records", async (req, res) => {
  const username = req.query.username;
  if (!username) return res.status(400).json({ error: "請提供 username" });

  try {
    const [rows] = await healthPool.query(
      `
      SELECT 
        u.display_name, u.age, u.gender,
        CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at,
        bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm,
        wr.weight, wr.height AS weight_height,
        CONVERT_TZ(wr.measured_at, '+00:00', '+08:00') AS weight_measured_at
      FROM BloodPressure bp
      JOIN Users u ON u.user_id = bp.user_id
      LEFT JOIN weight_records wr 
        ON wr.username COLLATE utf8mb4_unicode_ci = u.username COLLATE utf8mb4_unicode_ci
       AND DATE(CONVERT_TZ(bp.measure_at, '+00:00', '+08:00')) = DATE(CONVERT_TZ(wr.measured_at, '+00:00', '+08:00'))
      WHERE u.username = ?
      ORDER BY bp.measure_at ASC
      `,
      [username]
    );

    if (!rows.length) return res.status(404).json({ error: "沒有資料" });

    res.json(rows);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

// 取得單一疾病對應的來源 URL
app.get("/get_source_url", async (req, res) => {
  const disease = req.query.disease;
  if (!disease) return res.json({ url: "" });

  try {
    const [rows] = await exercisePool.query(
      `SELECT source_url 
       FROM exercise_suggestions 
       WHERE FIND_IN_SET(?, condition_combination)
       LIMIT 1`,
      [disease]
    );
    res.json({ url: rows[0]?.source_url || "" });
  } catch (e) {
    console.error(e);
    res.json({ url: "" });
  }
});

//-------------------------------------------(建二)-----------------------------------------------------

// 📦 取得所有藥物資料
app.get("/get_medications", async (req, res) => {
  let connection;
  try {
    connection = await pool.getConnection();
    const [rows] = await connection.query(
      `SELECT 
        m.id, 
        m.name, 
        mt.type_name AS type,
        m.dosage, 
        m.ingredients,
        m.contraindications, 
        m.side_effects,
        m.source_url
      FROM Medications m
      JOIN MedicationTypes mt ON m.type_id = mt.id`
    );
    res.json(rows);
  } catch (err) {
    console.error("❌ 資料庫查詢失敗:", err);
    res.status(500).json({ error: "資料庫查詢失敗" });
  } finally {
    if (connection) connection.release();
  }
});

//-------------------------------------------(建三)-----------------------------------------------------

app.get("/diseases", async (req, res) => {
  try {
    const [rows] = await pool.query("SELECT * FROM diseases");
    res.json(rows);
  } catch (err) {
    console.error("❌ 取得 diseases 失敗:", err);
    res.status(500).json({ error: err.message });
  }
});

// 🎬 查詢指定病症的影片與資料來源
app.get("/diseases/:disease_id/videos", async (req, res) => {
  const diseaseId = req.params.disease_id;
  try {
    const [rows] = await pool.query(
      `SELECT
         category,
         title,
         video_url,
         reference_url
       FROM disease_videos
       WHERE disease_id = ?`,
      [diseaseId]
    );
    res.json(rows);
  } catch (err) {
    console.error(`❌ 取得 disease_id=${diseaseId} 的影片失敗:`, err);
    res.status(500).json({ error: err.message });
  }
});

// ✅ 加入醫院掛號 API
app.get("/hospitals", async (req, res) => {
  const region = req.query.region;
  if (!region) {
    return res
      .status(400)
      .json({ error: "請提供 region 參數，如 ?region=台北" });
  }

  try {
    const [rows] = await pool.query(
      `SELECT id, name, region, latitude, longitude, url FROM hospitals WHERE region = ?`,
      [region]
    );
    res.json(rows);
  } catch (err) {
    console.error("❌ 查詢醫院失敗:", err);
    res.status(500).json({ error: "資料庫查詢失敗" });
  }
});

// 🧠 改良版分段工具（table 為單位）
function splitJsonByTable(obj, maxLength = 6000) {
  const chunks = [];
  let current = {};
  let size = 0;

  for (const key in obj) {
    const str = JSON.stringify({ [key]: obj[key] });
    if (size + str.length > maxLength) {
      chunks.push(current);
      current = {};
      size = 0;
    }
    current[key] = obj[key];
    size += str.length;
  }

  if (Object.keys(current).length > 0) {
    chunks.push(current);
  }

  return chunks;
}

// ✅ 改為串接 Azure GPT（取代 openai.com）
app.post("/api/chat", async (req, res) => {
  const { message, username } = req.body;
  if (!message || !username) {
    return res.status(400).json({ error: "請提供 message 和 username" });
  }

  // 🔎 Log request payload
  console.log("📩 使用者輸入:", { message, username });

  // 🔎 Log env variables (只顯示前幾碼避免洩漏)
  console.log("🔑 環境變數檢查:", {
    AZURE_ENDPOINT: process.env.AZURE_ENDPOINT,
    DEPLOYMENT_NAME: process.env.DEPLOYMENT_NAME,
    API_VERSION: process.env.API_VERSION,
    AZURE_API_KEY: process.env.AZURE_API_KEY
      ? process.env.AZURE_API_KEY.substring(0, 8) + "...(hidden)"
      : "❌ 未設定",
  });

  // ✅ 一次讓 GPT 判斷是否為健康問題＋是否要查資料庫
  const { isHealth, needsDatabase, isChitchat } = await (async () => {
    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;

    // 🔎 Log request URL
    console.log("🌐 GPT 判斷請求 URL:", url);

    const messages = [
      {
        role: "system",
        content: `你是一個 JSON 回傳機器。請針對使用者的提問，回傳以下格式的 JSON（不要多說話）：
        {
          "isHealth": true/false,
          "needsDatabase": true/false,
          "isChitchat": true/false
        }`,
      },
      { role: "user", content: `使用者提問：「${message}」` },
    ];

    try {
      const response = await axios.post(
        url,
        { messages, temperature: 0 },
        {
          headers: {
            "Content-Type": "application/json",
            "api-key": AZURE_API_KEY,
          },
          timeout: 15000,
        }
      );

      // 🔎 Log GPT response 原始內容
      console.log("✅ GPT 判斷回應:", response.data);

      const content = response.data.choices[0].message.content.trim();
      const parsed = JSON.parse(content);
      return {
        isHealth: parsed.isHealth === true,
        needsDatabase: parsed.needsDatabase === true,
        isChitchat: parsed.isChitchat === true,
      };
    } catch (err) {
      console.error("❌ GPT 判斷失敗：", err.message);
      if (err.response) {
        console.error("📦 錯誤詳細:", err.response.data);
      }
      return { isHealth: false, needsDatabase: false, isChitchat: false };
    }
  })();

  // ✅ 閒聊模式
  if (isChitchat) {
    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;
    console.log("🌐 GPT 陪聊請求 URL:", url);

    const messages = [
      {
        role: "system",
        content: `你是一位溫柔的心靈陪伴者，會用溫暖、理解、充滿同理心的語氣回應使用者。
        請像朋友一樣與使用者對話，避免過於醫學化或知識性太強的語句。`,
      },
      { role: "user", content: message },
    ];

    try {
      const response = await axios.post(
        url,
        { messages, temperature: 0.8 },
        {
          headers: {
            "Content-Type": "application/json",
            "api-key": AZURE_API_KEY,
          },
          timeout: 20000,
        }
      );

      console.log("✅ GPT 陪聊回應:", response.data);

      const reply = response.data.choices[0].message.content;
      return res.json({ reply });
    } catch (error) {
      console.error("❌ GPT 陪聊模式失敗：", error.message);
      if (error.response) {
        console.error("📦 錯誤詳細:", error.response.data);
      }
      return res.status(500).json({ error: "GPT 陪伴模式回覆失敗" });
    }
  }

  // ✅ 非健康問題
  if (!isHealth) {
    return res.json({ reply: "⚠️ 抱歉，我目前只回覆健康與醫療相關的問題唷！" });
  }

  // ✅ 健康問題（不查資料庫）
  if (!needsDatabase) {
    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;
    console.log("🌐 GPT 健康回覆請求 URL:", url);

    const messages = [
      {
        role: "system",
        content: `你是一位親切的健康小幫手，請用簡單明瞭的方式回答問題，回覆格式：
        1. 簡短總結
        2. 重點分析（最多三點）
        3. 建議（最多一段話）`,
      },
      { role: "user", content: `使用者提問：「${message}」` },
    ];

    try {
      const response = await axios.post(
        url,
        { messages, temperature: 0.7 },
        {
          headers: {
            "Content-Type": "application/json",
            "api-key": AZURE_API_KEY,
          },
          timeout: 20000,
        }
      );

      console.log("✅ GPT 健康回應:", response.data);

      const reply = response.data.choices[0].message.content;
      return res.json({ reply });
    } catch (error) {
      console.error("❌ GPT 回覆錯誤：", error.message);
      if (error.response) {
        console.error("📦 錯誤詳細:", error.response.data);
      }
      return res.status(500).json({ error: "GPT 回覆失敗" });
    }
  }

  // ✅ 健康問題 + 需要查資料庫（此段我保持原本程式，只加少量 log）
  const allData = {};
  let connection;
  try {
    connection = await pool.getConnection();
    console.log("✅ 成功連線到資料庫");

    const tables = ["BloodPressure", "weight_records", "AnxietyIndex"];
    for (const table of tables) {
      try {
        console.log(`🔍 查詢資料表: ${table}`);
        let query = `SELECT * FROM ${table}`;
        // ...（這裡保留你原本的 SQL 查詢）
        const [rows] = await connection.query(query);
        allData[table] = rows;
      } catch (err) {
        console.warn(`⚠️ 查詢 ${table} 發生錯誤：`, err.message);
        allData[table] = [];
      }
    }

    connection.release();
    console.log("✅ 資料庫查詢完成");
  } catch (err) {
    console.error("❌ 資料庫連線失敗：", err.message);
  }

  // ...（後續 GPT with DB 的邏輯不變）
});

// ✅ 每日語錄 API：簡短 + 美化後回傳
app.get("/api/daily-quote", async (req, res) => {
  const today = new Date().toISOString().slice(0, 10);

  const messages = [
    {
      role: "system",
      content: `你是一位健康語錄產生器，每天提供一句簡短且激勵人心的健康語錄，風格輕鬆、正向，並且只使用一個 emoji，請確保語錄簡潔，並保證僅顯示在一行內，請直接輸出語錄內容，不要加上任何額外說明。例如：\n\n今天也是活力滿滿的一天，記得動一動喔！💪`,
    },
    {
      role: "user",
      content: `請給我 ${today} 的每日語錄`,
    },
  ];

  try {
    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;
    const response = await axios.post(
      url,
      {
        messages,
        temperature: 0.8,
      },
      {
        headers: {
          "Content-Type": "application/json",
          "api-key": AZURE_API_KEY,
        },
        timeout: 20000,
      }
    );

    const rawQuote = response.data.choices[0].message.content.trim();
    const fullQuote = rawQuote;
    res.json({
      quote: fullQuote, // ✅ 只回傳語錄
    });
  } catch (err) {
    console.error("❌ 每日語錄產生失敗：", err.message);
    res.status(500).json({ error: "取得每日語錄失敗" });
  }
});

//----------------------------------------(建新增)----------------------------------------------------

app.post("/analyzeCombinedRecords", async (req, res) => {
  const {
    focus = "ALL",
    systolic_mmHg,
    diastolic_mmHg,
    pulse_bpm,
    weight_kg,
    height_cm,
    gender,
    age,
    measured_at,
  } = req.body || {};

  // 🔸 安全轉大寫、驗證 focus 參數
  const FOCUS = ["BP", "PULSE", "WEIGHT", "ALL"].includes(
    String(focus).toUpperCase()
  )
    ? String(focus).toUpperCase()
    : "ALL";

  // 🔸 若全空則回錯誤
  if (
    FOCUS === "ALL" &&
    !systolic_mmHg &&
    !diastolic_mmHg &&
    !pulse_bpm &&
    !weight_kg
  ) {
    return res.status(400).json({ error: "請提供至少一筆健康資料" });
  }

  // 🔸 判斷各欄位是否輸出
  const sysStr =
    FOCUS === "BP" || FOCUS === "ALL" ? systolic_mmHg ?? "無" : "無";
  const diaStr =
    FOCUS === "BP" || FOCUS === "ALL" ? diastolic_mmHg ?? "無" : "無";
  const pulseStr =
    FOCUS === "PULSE" || FOCUS === "ALL" ? pulse_bpm ?? "無" : "無";
  const wStr = FOCUS === "WEIGHT" || FOCUS === "ALL" ? weight_kg ?? "無" : "無";

  // 🔸 顯示文字對應
  const focusHuman = {
    BP: "血壓（收縮壓與舒張壓）",
    PULSE: "脈搏",
    WEIGHT: "體重",
    ALL: "當日重點指標",
  }[FOCUS];

  // 🔸 GPT 系統提示
  const systemPrompt = `
  你是健康數據分析師，請根據提供的數據，回傳下列其中一項分類結果，格式如下：

  「分類，建議」

  分類只能是以下其中一種：
    - 高血壓
    - 血壓偏高
    - 低血壓
    - 脈搏太高
    - 脈搏太低
    - 血壓正常
    - 脈搏正常
    - 體重正常

    建議部分請給一句繁體中文健康生活建議（約 20 字內）。

    請根據 focus 指定的重點（BP / PULSE / WEIGHT）進行判斷：
    - focus=BP：請根據收縮壓與舒張壓判斷
    - focus=PULSE：請根據脈搏判斷
    - focus=WEIGHT：請根據體重、身高、性別、年齡計算 BMI 判斷
    
    嚴格規範：
    - 僅能回傳「分類，建議」這一句話
    - 不得出現多餘說明、理由、語助詞、Markdown 或 JSON
    `.trim();

  const userPrompt = `
  日期：${measured_at || "未知"}
  收縮壓（mmHg）：${sysStr}
  舒張壓（mmHg）：${diaStr}
  脈搏（bpm）：${pulseStr}
  體重（kg）：${wStr}
  身高（cm）：${height_cm || "無"}
  性別：${gender || "無"}
  年齡：${age || "無"}
  `.trim();

  const messages = [
    { role: "system", content: systemPrompt },
    { role: "user", content: userPrompt },
  ];

  const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;

  // 🔸 狀態分類邏輯
  function classifyStatus({
    systolic_mmHg,
    diastolic_mmHg,
    pulse_bpm,
    weight_kg,
    height_cm,
  }) {
    if (systolic_mmHg >= 140 || diastolic_mmHg >= 90) return "高血壓";
    if (
      (systolic_mmHg >= 130 && systolic_mmHg < 140) ||
      (diastolic_mmHg >= 85 && diastolic_mmHg < 90)
    )
      return "血壓偏高";
    if (systolic_mmHg < 90 || diastolic_mmHg < 60) return "低血壓";
    if (pulse_bpm > 120) return "脈搏太高";
    if (pulse_bpm < 50) return "脈搏太低";
    if (weight_kg && height_cm) {
      const bmi = weight_kg / (height_cm / 100) ** 2;
      if (bmi > 24) return "體重過重";
      if (bmi < 18.5) return "體重過輕";
    }
    return "正常";
  }

  try {
    const response = await axios.post(
      url,
      {
        messages,
        temperature: 0.6,
        max_tokens: 500,
      },
      {
        headers: {
          "Content-Type": "application/json",
          "api-key": AZURE_API_KEY,
        },
      }
    );

    const suggestion =
      response.data.choices?.[0]?.message?.content?.trim() ||
      "⚠️ GPT 回傳內容為空白";
    const status = classifyStatus({
      systolic_mmHg,
      diastolic_mmHg,
      pulse_bpm,
      weight_kg,
      height_cm,
    });

    res.json({ status, suggestion });
  } catch (e) {
    console.error("GPT 錯誤：", e.response?.data || e.message);
    res.status(500).json({ error: "❌ GPT 分析失敗" });
  }
});
// 常見地區映射（可擴充）
const REGION_MAP = {
  台北: "台北",
  臺北: "台北",
  台北市: "台北",
  臺北市: "台北",
  新北: "新北",
  新北市: "新北",
  桃園: "桃園",
  桃園市: "桃園",
  新竹: "新竹",
  新竹市: "新竹",
  新竹縣: "新竹",
  台中: "台中",
  臺中: "台中",
  台中市: "台中",
  臺中市: "台中",
  台南: "台南",
  臺南: "台南",
  高雄: "高雄",
  高雄市: "高雄",
};

function normalizeText(s = "") {
  return String(s)
    .trim()
    .replace(/[臺台]/g, "台")
    .replace(/\s+/g, "")
    .toLowerCase();
}

// 從口語文字抓地區（例如：「桃園」、「台北長庚」會抓出桃園或台北）
function parseRegionFromText(text = "") {
  for (const k of Object.keys(REGION_MAP)) {
    if (text.includes(k)) return REGION_MAP[k];
  }
  return ""; // 沒講就留空
}

// 依「名稱 + 優先地區」找醫院（優先回傳指定地區的分院）
async function findHospitalByNameWithRegion(db, name, regionPreferred = "") {
  const [rows] = await db.query(
    "SELECT id, name, region, url FROM hospitals LIMIT 1000"
  );

  const targetName = normalizeText(name);
  const targetRegion = normalizeText(regionPreferred);

  const withRegion = [];
  const others = [];

  for (const r of rows) {
    const nm = normalizeText(r.name);
    const rg = normalizeText(r.region);

    // 非常鬆的名稱相似度：完全相等 > 互相包含
    const score =
      nm === targetName
        ? 1.0
        : nm.includes(targetName) || targetName.includes(nm)
        ? 0.9
        : 0.0;

    if (score === 0) continue;
    (rg === targetRegion ? withRegion : others).push({ ...r, _score: score });
  }

  const pickBest = (arr) => arr.sort((a, b) => b._score - a._score)[0] || null;
  return pickBest(withRegion) || pickBest(others) || null;
}

// === ★ AI 自動化：語意解析 ===
app.post("/api/ai/automation", async (req, res) => {
  const { username, text } = req.body || {};
  if (!username || !text)
    return res.status(400).json({ ok: false, error: "username 與 text 必填" });

  try {
    const [hospitals] = await db.query(
      "SELECT id, name FROM hospitals LIMIT 100"
    );
    const hospitalsContext = hospitals
      .map((h) => `${h.id}:${h.name}`)
      .join("\n");

    const SYSTEM_PROMPT = `
你是健康管理 App 的「語意解析器」。只輸出 JSON，不要任何多餘文字或 Markdown。
時間請以 Asia/Taipei 轉換「今天/明天/後天」為 YYYY-MM-DD；無時間預設 08:00；無日期預設今天。
僅允許 intent 為：book_hospital | unknown
JSON 結構如下：
{
  "ok": true/false,
  "intent": "book_hospital | unknown",
  "username": "string",
  "payload": {
    "title": "string",
    "notes": "string",
    "date": "YYYY-MM-DD",
    "time": "HH:mm",
    "hospital": {
      "hospital_id": number,
      "name": "string",
      "region": "string",
      "department": "string",
      "doctor": "string"
    },
    "navigatesTo": "MainActivity5"
  },
  "speakback": "string"
}
判斷參考：
- 出現「掛號、預約門診、看診」→ intent 設為 book_hospital（navigatesTo=MainActivity5）
請嚴格輸出 JSON。
`.trim();
    const userPrompt = `
使用者：${username}
可參考醫院清單（部分）：\n${hospitalsContext}
語句：${text}
請依結構嚴格輸出 JSON。
    `.trim();

    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;
    const response = await axios.post(
      url,
      {
        response_format: { type: "json_object" },
        temperature: 0.1,
        max_tokens: 500,
        messages: [
          { role: "system", content: SYSTEM_PROMPT },
          { role: "user", content: userPrompt },
        ],
      },
      {
        headers: {
          "Content-Type": "application/json",
          "api-key": AZURE_API_KEY,
        },
        timeout: 20000,
      }
    );

    const parsed = JSON.parse(
      response.data.choices?.[0]?.message?.content || "{}"
    );
    parsed.username = username;

    // ★★★ 補上：若意圖是掛號，依地區挑對分院（避免回到台北分院）
    if (parsed.intent === "book_hospital") {
      // 1) 從 LLM 結果或原句中取得「醫院名稱」與「地區」
      const userText = text || "";
      const llmHospitalName = parsed?.payload?.hospital?.name || ""; // 如果 LLM 有給名稱
      const regionFromLLM = parsed?.payload?.hospital?.region || ""; // 如果 LLM 有給地區
      const regionFromText = parseRegionFromText(userText); // 從口語再抓一次

      const finalRegion = regionFromLLM || regionFromText || ""; // 例如「桃園」

      // 2) 有醫院名稱才找；沒有名稱時你也可以用簡單關鍵字去抓（此處簡化）
      const hospitalNameHint = llmHospitalName || ""; // 建議也把「長庚」這種關鍵字交給 LLM 生成

      if (hospitalNameHint) {
        const matched = await findHospitalByNameWithRegion(
          db,
          hospitalNameHint,
          finalRegion
        );
        if (matched) {
          parsed.payload = parsed.payload || {};
          parsed.payload.hospital = {
            ...(parsed.payload.hospital || {}),
            hospital_id: matched.id,
            name: matched.name,
            region: matched.region,
          };
          parsed.payload.hospital_url = matched.url || "";
        }
      }

      // 3) 即使沒找到也把 region 放入 payload，讓前端能切對地區
      parsed.payload = parsed.payload || {};
      parsed.payload.hospital = parsed.payload.hospital || {};
      if (!parsed.payload.hospital.region && finalRegion) {
        parsed.payload.hospital.region = finalRegion;
      }
    }

    return res.json(parsed);
  } catch (err) {
    console.error(
      "❌ /api/ai/automation 失敗：",
      err.response?.data || err.message
    );
    return res.status(500).json({ ok: false, error: "automation_failed" });
  }
});

//----------------------------------------(註冊家庭)----------------------------------------------------

// 1️⃣ 註冊
app.get("/register", async (req, res) => {
  const {
    username: u,
    password: p,
    display_name: dn,
    age,
    gender,
    height,
    weight,
  } = req.query;
  if (!u || !p || !dn || !age || !gender) {
    return res.status(400).json({
      error:
        "請帶齊 username, password, display_name, age, gender,height,weight",
      usage:
        "/register?username=<帳號>&password=<密碼>&display_name=<暱稱>&age=21&gender=男",
    });
  }
  try {
    const hashedPw = hashPw(p);
    const sql =
      "INSERT INTO Users (username, password, display_name, age, gender,height,weight) VALUES (?, ?, ?, ?, ?, ?, ?)";
    const [result] = await db.query(sql, [
      u,
      hashedPw,
      dn,
      age,
      gender,
      height,
      weight,
    ]);

    const token = genToken(u);
    return res.status(201).json({ message: "註冊成功", token });
  } catch (err) {
    // mysql 重複鍵錯誤碼是 ER_DUP_ENTRY (errno: 1062)
    if (err.errno === 1062) {
      return res.status(409).json({ error: "帳號已存在" });
    }
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 2️⃣ 登入
app.get("/login", async (req, res) => {
  const { username: u, password: p } = req.query;
  if (!u || !p) {
    return res.status(400).json({
      error: "請帶齊 username, password",
      usage: "/login?username=<帳號>&password=<密碼>",
    });
  }
  try {
    const hashedPw = hashPw(p);
    const sql = "SELECT 1 AS ok FROM Users WHERE username = ? AND password = ?";
    const [rows] = await db.query(sql, [u, hashedPw]);

    if (rows.length === 0) {
      return res.status(401).json({ error: "帳號或密碼錯誤" });
    }
    const token = genToken(u);
    return res.json({ message: "登入成功", token });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 3️⃣ 驗證 Token
app.get("/verify", (req, res) => {
  const token = req.query.token || "";
  if (!token) {
    return res.status(400).json({
      error: "請帶 token 參數",
      usage: "/verify?token=<JWT>",
    });
  }
  try {
    const decoded = jwt.verify(token, JWT_SECRET_KEY);
    return res.json({ valid: true, username: decoded.username });
  } catch (err) {
    return res
      .status(401)
      .json({ valid: false, error: "Invalid or expired token" });
  }
});

// 4️⃣ 列家庭
app.get("/families", tokenRequired, async (req, res) => {
  try {
    // db 是你 mysql2.createPool() 建立的連線池
    const [rows] = await db.query(
      "SELECT family_id, family_name FROM Families"
    );

    // map 出你想要的欄位
    const items = rows.map((r) => ({
      family_id: r.family_id,
      family_name: r.family_name,
    }));

    return res.json(items);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 5️⃣ 創家庭
app.get("/families/create", tokenRequired, async (req, res) => {
  const name = (req.query.family_name || "").trim();
  if (!name) {
    return res.status(400).json({ error: "請帶 family_name" });
  }

  try {
    // 先查詢 user_id
    const [userRows] = await db.query(
      "SELECT user_id FROM Users WHERE username = ?",
      [req.user]
    );

    if (userRows.length === 0) {
      return res.status(400).json({ error: "使用者不存在" });
    }

    const uid = userRows[0].user_id;

    // 新增家庭，並取得新增的 family_id
    const [insertResult] = await db.query(
      "INSERT INTO Families (family_name, created_by) VALUES (?, ?)",
      [name, uid]
    );

    // insertResult.insertId 是 MySQL 自動產生的 ID (family_id)
    const fid = insertResult.insertId;

    // 把建立者加入家庭成員
    await db.query(
      "INSERT INTO FamilyMembers (family_id, user_id) VALUES (?, ?)",
      [fid, uid]
    );

    return res.status(201).json({ message: "家庭創建成功", family_id: fid });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 6️⃣ 加入家庭
app.get("/families/join", tokenRequired, async (req, res) => {
  const fid = parseInt(req.query.family_id);
  if (!fid) {
    return res.status(400).json({ error: "請帶 family_id(int)" });
  }

  try {
    // 查 user_id
    const [userRows] = await db.query(
      "SELECT user_id FROM Users WHERE username = ?",
      [req.user]
    );

    if (userRows.length === 0) {
      return res.status(400).json({ error: "使用者不存在" });
    }

    const uid = userRows[0].user_id;

    // 確認是否已加入該家庭
    const [existRows] = await db.query(
      "SELECT 1 AS ok FROM FamilyMembers WHERE family_id = ? AND user_id = ?",
      [fid, uid]
    );

    if (existRows.length > 0) {
      return res.json({ message: "已在此家庭中" });
    }

    // 新增家庭成員
    await db.query(
      "INSERT INTO FamilyMembers (family_id, user_id) VALUES (?, ?)",
      [fid, uid]
    );

    return res.status(201).json({ message: "加入成功" });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 7️⃣ 家庭成員列表
app.get("/families/members", tokenRequired, async (req, res) => {
  try {
    // 先查出 user_id
    const [userRows] = await db.query(
      "SELECT user_id FROM Users WHERE username = ?",
      [req.user]
    );
    if (userRows.length === 0) {
      return res.status(400).json({ error: "使用者不存在" });
    }
    const uid = userRows[0].user_id;

    // 查該 user 所屬家庭的所有成員
    const [rows] = await db.query(
      `SELECT fm.family_id, u.user_id, u.display_name
       FROM FamilyMembers fm
       JOIN Users u ON fm.user_id = u.user_id
       WHERE fm.family_id IN (
         SELECT family_id FROM FamilyMembers WHERE user_id = ?
       )`,
      [uid]
    );

    const members = rows.map((r) => ({
      family_id: r.family_id,
      user_id: r.user_id,
      display_name: r.display_name,
    }));

    return res.json(members);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 8️⃣ 列提醒
app.get("/reminders/list", tokenRequired, async (req, res) => {
  try {
    // 先找 user_id
    const [userRows] = await db.query(
      "SELECT user_id FROM Users WHERE username = ?",
      [req.user]
    );
    if (userRows.length === 0) {
      return res.status(400).json({ error: "使用者不存在" });
    }
    const uid = userRows[0].user_id;

    // 查提醒列表
    const [rows] = await db.query(
      `SELECT r.reminder_id, r.family_id, r.hour, r.minute,
              r.category, r.dayOfWeek, r.isRepeat,
              r.title, r.content
       FROM Reminders r
       JOIN FamilyMembers fm ON r.family_id = fm.family_id
       WHERE fm.user_id = ?`,
      [uid]
    );

    const data = rows.map((r) => ({
      reminder_id: r.reminder_id,
      family_id: r.family_id,
      hour: r.hour,
      minute: r.minute,
      category: r.category,
      dayOfWeek: r.dayOfWeek,
      isRepeat: Boolean(r.isRepeat),
      title: r.title,
      content: r.content,
    }));

    return res.json(data);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// 9️⃣ 新增提醒
app.get("/reminders/add", tokenRequired, async (req, res) => {
  const hour = parseInt(req.query.hour);
  const minute = parseInt(req.query.minute);
  const cat = req.query.category || "";
  const title = req.query.title || "";
  const content = req.query.content || "";
  const rep = req.query.isRepeat === "1" ? 1 : 0;

  if (isNaN(hour) || isNaN(minute) || !cat || !title || !content) {
    return res
      .status(400)
      .json({ error: "請帶齊 hour, minute, category, title, content" });
  }

  try {
    // 取得 user_id
    const [userRows] = await db.query(
      "SELECT user_id FROM Users WHERE username = ?",
      [req.user]
    );
    if (userRows.length === 0) {
      return res.status(400).json({ error: "使用者不存在" });
    }
    const uid = userRows[0].user_id;

    // 取得使用者加入的第一個 family_id
    const [famRows] = await db.query(
      "SELECT family_id FROM FamilyMembers WHERE user_id = ? LIMIT 1",
      [uid]
    );
    if (famRows.length === 0) {
      return res.status(400).json({ error: "請先加入或創建家庭" });
    }
    const fid = famRows[0].family_id;

    // 插入提醒
    await db.query(
      `INSERT INTO Reminders 
      (family_id, hour, minute, category, dayOfWeek, isRepeat, title, content, created_by)
      VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)`,
      [fid, hour, minute, cat, rep, title, content, uid]
    );

    return res.status(201).json({ message: "提醒新增成功" });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: err.message });
  }
});

// ✅ 體重上傳 API
app.post("/upload-weight", async (req, res) => {
  const { username, weight, gender, height, age } = req.body;

  const sql = `
    INSERT INTO weight_records (username, weight, gender, height, age)
    VALUES (?, ?, ?, ?, ?)
  `;

  try {
    const [result] = await db.execute(sql, [
      username,
      weight,
      gender,
      height,
      age,
    ]);
    res.json({ message: "✅ 體重與基本資料已儲存", id: result.insertId });
  } catch (err) {
    console.error("❌ 資料庫寫入錯誤:", err);
    res.status(500).json({ error: "資料寫入失敗" });
  }
});

// ✅ 統一查詢 API（圖表、歷史）
app.get("/weight-history", async (req, res) => {
  const { username, start, end } = req.query;

  try {
    let sql = `
      SELECT 
        id,
        username,
        gender,
        height,
        age,
        weight,
        DATE_FORMAT(measured_at, '%Y-%m-%d %H:%i:%s') AS measured_at
      FROM weight_records
      WHERE 1=1
    `;
    const params = [];

    if (username) {
      sql += " AND username = ?";
      params.push(username);
    }

    if (start && end) {
      sql += " AND measured_at BETWEEN ? AND ?";
      params.push(`${start} 00:00:00`, `${end} 23:59:59`);
    }

    sql += " ORDER BY measured_at ASC";

    const [rows] = await db.execute(sql, params);
    res.json(rows);
  } catch (error) {
    console.error("❌ 查詢失敗：", error);
    res.status(500).json({ message: "查詢歷史資料失敗" });
  }
});

// 啟動伺服器
app.listen(port, () => {
  console.log(`伺服器正在運行於 http://localhost:${port}`);
});
