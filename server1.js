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
  host: process.env.MYSQL_HOST || "metro.proxy.rlwy.net",
  port: Number(process.env.MYSQL_PORT) || 31311,
  user: process.env.MYSQL_USER || "root",
  password: process.env.MYSQL_PASSWORD || "ftKBvQlApCeRAcMjPKkhbhfNIxhfGtfY",
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

  // 從資料庫撈出該 user_id 所有的資料
  const [existingRows] = await db.query(
    `
    SELECT measure_at, systolic_mmHg, diastolic_mmHg, pulse_bpm
    FROM BloodPressure
    WHERE user_id = ?
    `,
    [userId]
  );

  // 建立 Set 儲存所有現有的 key
  const existingSet = new Set(
    existingRows.map(
      (row) =>
        `${row.measure_at}_${row.systolic_mmHg}_${row.diastolic_mmHg}_${row.pulse_bpm}`
    )
  );

  // 檢查每筆新資料是否存在
  for (const row of rows) {
    const formattedDate = row["測量日期"].split("T")[0];
    const key = `${formattedDate}_${row["收縮壓(mmHg)"]}_${row["舒張壓(mmHg)"]}_${row["脈搏(bpm)"]}`;

    if (!existingSet.has(key)) {
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
        userId,
      ]);
    } else {
      console.log(`資料已存在，跳過插入: ${key}`);
    }
  }

  // 執行 INSERT
  if (valuesToInsert.length > 0) {
    const insertQuery = `
      INSERT INTO BloodPressure
      (measure_at, timezone, systolic_mmHg, diastolic_mmHg, pulse_bpm, irregular_pulse,
       irregular_count, motion_detected, cuff_tightness_ok, posture_ok, room_temp_c,
       test_mode, device_model, user_id)
      VALUES ?
    `;
    const [result] = await db.query(insertQuery, [valuesToInsert]);
    console.log(`✅ 成功插入 ${result.affectedRows} 筆資料`);
    return result;
  } else {
    console.log("✅ 沒有需要插入的新資料");
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

//-----------------------------------------(建一)-----------------------------------------------------

// ===== 分析邏輯函式 =====
function evaluateBloodPressure(systolic, diastolic) {
  if (systolic >= 140 || diastolic >= 90)
    return [
      "高血壓（危險）",
      "⚠️ 建議立即就醫、服藥與生活調整",
      "心血管影響、腎功能惡化、中風",
      "高級",
    ];
  if ((120 <= systolic && systolic < 130) || diastolic === 80)
    return [
      "血壓偏高",
      "🟡 留意生活壓力與鹽分攝取",
      "初期尚無症狀，應提早預防",
      "初級",
    ];
  if (systolic < 90 || diastolic < 60)
    return [
      "低血壓",
      "🌀 建議補充水分與營養，避免久站與劇烈運動",
      "可能出現疲倦、暈眩，甚至昏厥",
      "中級",
    ];
  if (90 <= systolic && systolic <= 120 && 60 <= diastolic && diastolic <= 80)
    return ["正常", "✅ 血壓正常，請持續維持健康生活", "無", "正常"];
  return [null, null, null, null];
}

function evaluatePulse(pulse) {
  if (pulse > 120)
    return [
      "脈搏過高",
      "可能有心律不整，建議就醫",
      "心律不整、焦慮、自律神經異常",
      "高級",
    ];
  if (pulse > 100)
    return ["高脈搏", "可能過度緊張或心臟問題", "心臟負荷過大", "中級"];
  if (pulse < 50)
    return ["脈搏過低", "可能心搏過緩", "心搏過緩、血流不足", "中級"];
  if (pulse < 60) return ["低脈搏", "需觀察是否頭暈疲勞", "暈眩、虛弱", "中級"];
  return [null, null, null, null];
}

function calculateOverallRisk(tags) {
  if (tags.includes("高級")) return "高級";
  if (tags.includes("中級")) return "中級";
  return "初級";
}

function crossInference(results) {
  const cross = [];
  if (results.includes("高血壓") && results.includes("高脈搏"))
    cross.push("高血壓合併高脈搏：心臟負擔過重，需控制血壓與心跳");
  if (results.includes("低血壓") && results.includes("低脈搏"))
    cross.push("低血壓合併低脈搏：可能為休克前兆，建議就醫");
  if (
    results.includes("肌低症") &&
    (results.includes("體重過低") || results.includes("體重偏輕"))
  )
    cross.push("肌肉流失與體重不足：有衰弱症風險");
  if (results.includes("高血壓") && results.includes("體重過重"))
    cross.push("代謝症候群風險上升");
  if (
    results.includes("高血壓") &&
    results.includes("高脈搏") &&
    results.includes("體重過重")
  )
    cross.push("三重風險：可能進入代謝症候群，需立即改善生活方式");
  return cross;
}

function analyzeRow(row) {
  const results = [],
    advices = [],
    diseases = [],
    tags = [];

  const bp = evaluateBloodPressure(row.systolic_mmHg, row.diastolic_mmHg);
  const pulse = evaluatePulse(row.pulse_bpm);

  [bp, pulse].forEach(([res, adv, dis, tag]) => {
    if (res) {
      results.push(res);
      advices.push(adv);
      diseases.push(dis);
      tags.push(tag);
    }
  });

  return {
    user: row.display_name,
    record_date: row.measure_at,
    age: row.age,
    gender: row.gender,
    風險等級: calculateOverallRisk(tags),
    分析結果: results,
    建議: advices,
    交叉狀況: crossInference(results),
    可能病症: diseases,
  };
}

async function analyzeAllAggregate(rows, userName) {
  const combinedResults = new Set();
  const combinedAdvices = new Set();
  const combinedCross = new Set();
  const combinedDiseases = new Set();
  const riskTags = [];

  rows.forEach((row) => {
    const r = analyzeRow(row);
    r.分析結果.forEach((x) => combinedResults.add(x));
    r.建議.forEach((x) => combinedAdvices.add(x));
    r.交叉狀況.forEach((x) => combinedCross.add(x));
    r.可能病症.forEach((x) => combinedDiseases.add(x));
    riskTags.push(r.風險等級);
  });

  return {
    user: userName,
    record_date: "全部資料",
    gender: rows[0]?.gender || "未知",
    age: rows[0]?.age || null,
    風險等級: calculateOverallRisk(riskTags),
    分析結果: Array.from(combinedResults),
    建議: Array.from(combinedAdvices),
    交叉狀況: Array.from(combinedCross),
    可能病症: Array.from(combinedDiseases),
  };
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

// 取得所有使用者
app.get("/get_users", async (req, res) => {
  try {
    const [rows] = await healthPool.query(
      `SELECT DISTINCT display_name FROM Users`
    );
    res.json(rows.map((r) => r.display_name));
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

// 取得指定使用者可用日期
app.get("/dates/:user", async (req, res) => {
  try {
    const [rows] = await healthPool.query(
      `SELECT DATE_FORMAT(bp.measure_at, '%Y-%m-%d') AS measure_at
       FROM BloodPressure bp
       JOIN Users u ON bp.user_id = u.user_id
       WHERE u.display_name = ?
       GROUP BY measure_at
       ORDER BY measure_at DESC`,
      [req.params.user]
    );
    res.json(rows.map((r) => r.measure_at));
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

// 單筆分析
app.get("/analyzeSingle", async (req, res) => {
  const { user, date } = req.query;
  if (!user || !date)
    return res.status(400).json({ error: "請提供 user 與 date" });

  try {
    const [rows] = await healthPool.query(
      `SELECT bp.*, u.height, u.weight
       FROM BloodPressure bp
       JOIN Users u ON bp.user_id = u.user_id
       WHERE u.display_name = ?
         AND DATE(CONVERT_TZ(bp.measure_at, '+00:00', '+08:00')) = ?
       LIMIT 1`,
      [user, date]
    );
    if (!rows.length) return res.status(404).json({ error: "找不到該筆紀錄" });

    res.json(analyzeRow(rows[0]));
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

app.get("/analyzeAllAggregate", async (req, res) => {
  const user = req.query.user;
  if (!user) return res.status(400).json({ error: "請提供 user" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender, u.height, u.weight,
              CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at,
              bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm
       FROM BloodPressure bp
       JOIN Users u ON bp.user_id = u.user_id
       WHERE u.display_name = ?
       ORDER BY bp.measure_at ASC`,
      [user]
    );

    if (!rows.length)
      return res.status(404).json({ error: "該使用者沒有任何紀錄" });

    res.json(await analyzeAllAggregate(rows, user));
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

app.get("/get_records", async (req, res) => {
  const user = req.query.user;
  if (!user) return res.status(400).json({ error: "請提供 user" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender, u.height, u.weight,
              CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at,
              bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm
       FROM BloodPressure bp
       JOIN Users u ON bp.user_id = u.user_id
       WHERE u.display_name = ?
       ORDER BY bp.measure_at ASC`,
      [user]
    );

    if (!rows.length) return res.status(404).json({ error: "沒有資料" });

    res.json(rows);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

app.get("/analyzeRange", async (req, res) => {
  const { user, start_date } = req.query;
  if (!user || !start_date)
    return res.status(400).json({ error: "請提供 user 與 start_date" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender, u.height, u.weight,
              CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at,
              bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm
       FROM BloodPressure bp
       JOIN Users u ON bp.user_id = u.user_id
       WHERE u.display_name = ? AND bp.measure_at >= ?
       ORDER BY bp.measure_at ASC`,
      [user, start_date]
    );

    if (!rows.length) return res.status(404).json({ error: "該期間無資料" });

    res.json(await analyzeAllAggregate(rows, user));
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: e.message });
  }
});

app.get("/analyze/custom_range", async (req, res) => {
  const { user, start, end } = req.query;
  if (!user || !start || !end)
    return res.status(400).json({ error: "缺少必要參數" });

  try {
    const [rows] = await healthPool.query(
      `SELECT u.display_name, u.age, u.gender, u.height, u.weight,
              CONVERT_TZ(bp.measure_at, '+00:00', '+08:00') AS measure_at,
              bp.systolic_mmHg, bp.diastolic_mmHg, bp.pulse_bpm
       FROM BloodPressure bp
       JOIN Users u ON bp.user_id = u.user_id
       WHERE u.display_name = ? AND bp.measure_at BETWEEN ? AND ?
       ORDER BY bp.measure_at ASC`,
      [user, start, end]
    );

    if (!rows.length) return res.status(404).json({ error: "查無資料" });

    res.json(await analyzeAllAggregate(rows, user));
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

  // ✅ 一次讓 GPT 判斷是否為健康問題＋是否要查資料庫
  const { isHealth, needsDatabase } = await (async () => {
    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;
    const messages = [
      {
        role: "system",
        content: `你是一個 JSON 回傳機器。請針對使用者的提問，回傳以下格式的 JSON（不要多說話）：
        {
          "isHealth": true/false,
          "needsDatabase": true/false
        }
        若與健康、醫療、疾病、量測、飲食、身體、心理有關，isHealth 為 true，否則 false。
        若需要查詢健康資料庫（例如血壓紀錄、體重紀錄等），needsDatabase 為 true，否則 false。`,
      },
      {
        role: "user",
        content: `使用者提問：「${message}」`,
      },
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

      const content = response.data.choices[0].message.content.trim();
      const parsed = JSON.parse(content);
      return {
        isHealth: parsed.isHealth === true,
        needsDatabase: parsed.needsDatabase === true,
      };
    } catch (err) {
      console.error("❌ GPT 判斷失敗：", err.message);
      return { isHealth: false, needsDatabase: false };
    }
  })();

  if (!isHealth) {
    return res.json({ reply: "⚠️ 抱歉，我目前只回覆健康與醫療相關的問題唷！" });
  }

  // ✅ 純健康問題（不查資料庫）
  if (!needsDatabase) {
    const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;
    // ✅ 先將 messages 定義為變數
    const messages = [
      {
        role: "system",
        content: `你是一位親切的健康小幫手，請用簡單明瞭的方式回答問題，回覆內容要適合一般人閱讀，避免太多醫學術語。回覆格式請遵守：
        1. **簡短總結**
        2. **重點分析（最多三點）**
        3. **建議（最多一段話）**
        內容清晰、口語自然、避免過長，適合在手機畫面閱讀。`,
      },
      {
        role: "user",
        content: `使用者提問：「${message}」`,
      },
    ];

    try {
      const response = await axios.post(
        url,
        {
          messages,
          temperature: 0.7,
        },
        {
          headers: {
            "Content-Type": "application/json",
            "api-key": AZURE_API_KEY,
          },
          timeout: 20000, // ⏱️ 可加入保險
        }
      );

      const reply = response.data.choices[0].message.content;
      return res.json({ reply });
    } catch (error) {
      console.error("❌ GPT 回覆錯誤：", error.message);
      return res.status(500).json({ error: "GPT 回覆失敗" });
    }
  }

  // ✅ 若是健康問題 + 需要查資料庫
  const allData = {};
  let connection;

  try {
    connection = await pool.getConnection();

    // 💡 只查這四個與測量有關的表格，減少資料量
    const tables = ["BloodPressure", "weight_records", "AnxietyIndex"];

    for (const table of tables) {
      try {
        let query = `SELECT * FROM ${table}`;

        if (table === "BloodPressure") {
          query = `
          SELECT 
            id, user_id,
            CONVERT_TZ(measure_at, '+00:00', '+08:00') AS measure_at,
            timezone, systolic_mmHg, diastolic_mmHg, pulse_bpm,
            irregular_pulse, irregular_count, motion_detected,
            cuff_tightness_ok, posture_ok, room_temp_c, test_mode, device_model
          FROM BloodPressure
        `;
        } else if (table === "WeightData") {
          query = `
          SELECT 
            id, user_id,
            CONVERT_TZ(measure_at, '+00:00', '+08:00') AS measure_at,
            weight_kg
          FROM WeightData
        `;
        } else if (table === "weight_records") {
          query = `
          SELECT 
            id, displayname, gender, height, age, weight,
            CONVERT_TZ(measured_at, '+00:00', '+08:00') AS measured_at
          FROM weight_records
        `;
        } else if (table === "AnxietyIndex") {
          query = `
          SELECT 
            id, user_id, score, suggestion,
            CONVERT_TZ(measure_at, '+00:00', '+08:00') AS measure_at
          FROM AnxietyIndex
        `;
        }

        const [rows] = await connection.query(query);
        allData[table] = rows;
      } catch (err) {
        console.warn(`⚠️ 查詢 ${table} 發生錯誤：`, err.message);
        allData[table] = [];
      }
    }

    connection.release();
    console.log("✅ 僅查詢有測量資料的表格完成");
  } catch (err) {
    console.error("❌ 資料庫連線失敗：", err.message);
  }

  // 🔀 分段傳入 GPT
  const jsonChunks = splitJsonByTable(allData, 6000);
  if (jsonChunks.length > 5) {
    return res.status(413).json({
      error: "⚠️ 資料量過大，請縮小查詢範圍（例如只查本週紀錄）",
    });
  }
  const messages = [
    {
      role: "system",
      content: "你是一位親切的健康小助手，根據使用者的健康資料進行分析與建議。",
    },
  ];

  jsonChunks.forEach((chunk, idx) => {
    messages.push({
      role: "user",
      content: `以下是使用者的健康資料（第 ${idx + 1} 段）：${JSON.stringify(
        chunk
      )}`,
    });
  });

  messages.push({
    role: "user",
    content: `使用者提問：「${message}」，請根據上面所有健康資料進行回覆。`,
  });

  const url = `${AZURE_ENDPOINT}/openai/deployments/${DEPLOYMENT_NAME}/chat/completions?api-version=${API_VERSION}`;

  try {
    const response = await axios.post(
      url,
      {
        messages: messages,
        temperature: 0.7,
      },
      {
        headers: {
          "Content-Type": "application/json",
          "api-key": AZURE_API_KEY,
        },
        timeout: 90000,
        maxContentLength: Infinity, // ✅ 防止內容限制爆掉
        maxBodyLength: Infinity,
      }
    );

    const reply = response.data.choices[0].message.content;
    res.json({
      reply,
      records: allData,
    });
  } catch (error) {
    const status = error.response?.status || 500;
    const detail = error.response?.data || error.message;
    console.error("❌ GPT API 錯誤：", detail);
    res.status(status).json({ error: detail });
  }
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
