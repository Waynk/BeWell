-- 一、 建立並切換到 TEST 資料庫
CREATE DATABASE IF NOT EXISTS `ABC2`
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
USE `ABC2`;

-- 二、 刪除舊表（順序不拘，依賴自動處理）
DROP TABLE IF EXISTS `disease_videos`;
DROP TABLE IF EXISTS `diseases`;
DROP TABLE IF EXISTS `Medications`;
DROP TABLE IF EXISTS `MedicationTypes`;
DROP TABLE IF EXISTS `weight_records`;
DROP TABLE IF EXISTS `BloodPressure`;
DROP TABLE IF EXISTS `AnxietyIndex`;
DROP TABLE IF EXISTS `FamilyMembers`;
DROP TABLE IF EXISTS `Families`;
DROP TABLE IF EXISTS `Conditions`;
DROP TABLE IF EXISTS `exercise_suggestions`;
DROP TABLE IF EXISTS `hospitals`;
DROP TABLE IF EXISTS `Users`;
DROP TABLE IF EXISTS `MedicalAppointments`;


-- 三、 建立 Users
CREATE TABLE `Users` (
  `user_id`      INT            NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(50)    NOT NULL UNIQUE,
  `password`     VARCHAR(255)   NOT NULL,
  `display_name` VARCHAR(50)    NOT NULL,
  `created_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `age`          INT         NOT NULL,
  `gender`       ENUM('男','女') NOT NULL,
   `height`       FLOAT           DEFAULT NULL,  -- 👈 新增：身高（公分）
  `weight`       FLOAT           DEFAULT NULL,  -- 👈 新增：體重（公斤）
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 四、 建立 Families
CREATE TABLE `Families` (
  `family_id`   INT            NOT NULL AUTO_INCREMENT,
  `family_name` VARCHAR(100)   NOT NULL,
  `created_by`  INT            NOT NULL,
  `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`family_id`),
  CONSTRAINT `FK_Families_Users`
    FOREIGN KEY (`created_by`) REFERENCES `Users`(`user_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 五、 建立 FamilyMembers
CREATE TABLE `FamilyMembers` (
  `family_id` INT       NOT NULL,
  `user_id`   INT       NOT NULL,
  `joined_at` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`family_id`,`user_id`),
  CONSTRAINT `FK_FM_Families`
    FOREIGN KEY (`family_id`) REFERENCES `Families`(`family_id`)
    ON DELETE CASCADE,
  CONSTRAINT `FK_FM_Users`
    FOREIGN KEY (`user_id`) REFERENCES `Users`(`user_id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 八、 建立「血壓」表
CREATE TABLE `BloodPressure` (
 id                   INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id              INT           NOT NULL,
  measure_at           DATE          NOT NULL,
  timezone             VARCHAR(50),
  systolic_mmHg        INT           NOT NULL,
  diastolic_mmHg       INT           NOT NULL,
  pulse_bpm            INT           DEFAULT NULL,
  irregular_pulse      VARCHAR(20)   DEFAULT NULL,
  irregular_count      VARCHAR(50)   DEFAULT '0',
  motion_detected      VARCHAR(20)   DEFAULT '',
  cuff_tightness_ok    VARCHAR(20)   DEFAULT '',
  posture_ok           VARCHAR(20)   DEFAULT '',
  room_temp_c          DOUBLE        DEFAULT NULL,
  test_mode            VARCHAR(20)   DEFAULT NULL,
  device_model         VARCHAR(30)   DEFAULT NULL,

  CONSTRAINT FK_BP_Users
    FOREIGN KEY (`user_id`)
    REFERENCES `Users`(`user_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- 九、 建立「焦慮指數」表
CREATE TABLE `AnxietyIndex` (
  id           INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id      INT         NOT NULL,
  measure_at   DATE        NOT NULL,
  score        INT         NOT NULL,
  suggestion   VARCHAR(50) NOT NULL,
  CONSTRAINT FK_Anxiety_Users
    FOREIGN KEY (`user_id`)
    REFERENCES `Users`(`user_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- 🔟 建立藥物類型表
CREATE TABLE `MedicationTypes` (
  `id`        INT            NOT NULL AUTO_INCREMENT,
  `type_name` VARCHAR(100)   NOT NULL UNIQUE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⓫ 建立藥物表
CREATE TABLE `Medications` (
  `id`                INT            NOT NULL AUTO_INCREMENT,
  `name`              VARCHAR(100)   NOT NULL,
  `type_id`           INT            NOT NULL,
  `dosage`            VARCHAR(50)    NOT NULL,
  `frequency`         VARCHAR(50)    NOT NULL,
  `ingredients`       TEXT           NOT NULL,
  `contraindications` TEXT           NOT NULL,
  `side_effects`      TEXT           NOT NULL,
  `source_url`        TEXT           NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_Medications_Types`
    FOREIGN KEY (`type_id`) REFERENCES `MedicationTypes`(`id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⓬ 建立病症表
CREATE TABLE `diseases` (
  `id`          INT AUTO_INCREMENT PRIMARY KEY,
  `name`        VARCHAR(50) NOT NULL,
  `description` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⓭ 建立病症影片表
CREATE TABLE disease_videos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    disease_id INT NOT NULL,
    category ENUM(
        '預防', '症狀', '飲食', '改善',
        '病因', '診斷方法', '藥物與治療',
        '統計與案例', '醫師講解','心理調適', 
        '銀髮族專區'
    ) NOT NULL,
    title VARCHAR(100) NOT NULL,
    video_url TEXT NOT NULL,         
    reference_url TEXT,   
    FOREIGN KEY (disease_id) REFERENCES diseases(id) ON DELETE CASCADE
);

-- ✅ 體重紀錄資料表：weight_records
CREATE TABLE IF NOT EXISTS weight_records (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '紀錄編號',
    user_id INT NOT NULL COMMENT '使用者編號',
    display_name VARCHAR(100) COMMENT '顯示名稱',
    gender VARCHAR(10) COMMENT '性別',
    height FLOAT COMMENT '身高（cm）',
    age INT COMMENT '年齡',
    weight FLOAT COMMENT '體重（公斤）',
    measured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '測量時間',
    CONSTRAINT FK_WR_Users
      FOREIGN KEY (`user_id`) REFERENCES `Users`(`user_id`)
      ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;


INSERT INTO weight_records (username, gender, height, age, weight)
VALUES 
('黃明建', '男', 175.0, 23, 68.4),
('黃明建', '男', 175.0, 23, 52.3),
('黃明建', '男', 175.0, 23, 85.7),
('黃明建', '男', 175.0, 23, 49.8),
('黃明建', '男', 175.0, 23, 60.0);


-- 插入病症資料
INSERT INTO diseases (id, name, description) VALUES
(1, '高血壓', '血壓過高，可能造成心臟病或中風'),
(2, '低血壓', '血壓過低，容易頭暈、虛弱'),
(3, '高脈搏', '脈搏高於正常值，心臟可能過度負荷'),
(4, '低脈搏', '脈搏過低，心輸出可能不足'),
(5, '體重過高', '可能導致三高、心血管疾病'),
(6, '體重過低', '營養不良，免疫力下降');


INSERT INTO disease_videos (disease_id, category, title, video_url, reference_url) VALUES
-- 高血壓
( 1, '預防', '高血壓 - 預防教學', '​​https://www.youtube.com/watch?v=oDVAaum-i0Y', 'https://www.hpa.gov.tw/Pages/List.aspx?nodeid=1463'),
( 1, '症狀', '高血壓 - 症狀教學', '​https://www.youtube.com/watch?v=Ix9WxTLMIig', 'https://www.mayoclinic.org/zh-hans/diseases-conditions/high-blood-pressure/symptoms-causes/syc-20373410'),
( 1, '飲食', '高血壓 - 飲食教學', 'https://www.youtube.com/watch?v=6BIeSqt_YYY', 'https://www.healthlinkbc.ca/sites/default/files/documents/hfile68b-C.pdf'),
( 1, '改善', '高血壓 - 改善教學', '​https://www.youtube.com/watch?v=dh1dY6G8pOM', 'https://safe.ypu.edu.tw/p/412-1012-301.php'),
( 1, '病因', '高血壓 - 病因教學', '​https://www.youtube.com/watch?v=5RGbV4_NXC4', 'https://www.who.int/zh/news-room/fact-sheets/detail/hypertension'),
( 1, '診斷方法', '高血壓 - 診斷方法教學', 'https://www.youtube.com/watch?v=VIfjhviDw0I', 'https://www.mayoclinic.org/zh-hans/diseases-conditions/high-blood-pressure/diagnosis-treatment/drc-20373417'),
( 1, '藥物與治療', '高血壓 - 藥物與治療教學', 'https://www.youtube.com/watch?v=VpoR7g4B2SU', 'https://www.stdhc.org.hk/express/hypertension-drugs/'),
( 1, '統計與案例', '高血壓 - 統計與案例教學', 'https://www.youtube.com/watch?v=hMhXGCxIL2g', 'https://www.mohw.gov.tw/cp-2627-19188-1.html'),
( 1, '醫師講解', '高血壓 - 醫師講解教學', 'https://www.youtube.com/watch?v=SDh54sluLng', 'https://media.ntuh.gov.tw/media/809?autoplay=yes'),
( 1, '心理調適', '高血壓 - 心理調適教學', 'https://www.youtube.com/watch?v=ytDqak5pQDw', 'https://big5.cctv.com/gate/big5/news.cctv.com/2022/10/08/ARTIbByqOjTA3P3QNLlEVfyJ221008.shtml'),
( 1, '銀髮族專區', '高血壓 - 銀髮族專區教學', 'https://www.youtube.com/playlist?list=PLQoD7p0ICvjdEw89QkWiD_GTjBI8_u6FR', 'https://www.mohw.gov.tw/cp-5016-59550-1.html'),
 
-- 低血壓
( 2, '預防', '低血壓 - 預防教學', 'https://www.youtube.com/watch?v=j7o5d3z-7O0', 'https://www.healthnews.com.tw/article/62112'),
( 2, '症狀', '低血壓 - 症狀教學', 'https://www.youtube.com/watch?v=Eg19OcUV1i8', 'https://www.mayoclinic.org/zh-hans/diseases-conditions/low-blood-pressure/symptoms-causes/syc-20355465'),
( 2, '飲食', '低血壓 - 飲食教學', 'https://www.youtube.com/watch?v=l0MEfj1S6pI', 'https://www.top1health.com/Article/21953'),
( 2, '改善', '低血壓 - 改善教學', 'https://www.youtube.com/watch?v=j7o5d3z-7O0', 'https://helloyishi.com.tw/heart-health/hypertension/hypotension/'),
( 2, '病因', '低血壓 - 病因教學', 'https://www.youtube.com/watch?v=8CFtJlbZdqw', 'https://www.mayoclinic.org/zh-hans/diseases-conditions/low-blood-pressure/symptoms-causes/syc-20355465'),
( 2, '診斷方法', '低血壓 - 診斷方法教學', 'https://www.youtube.com/watch?v=dlQIKO0AGf8', 'https://www.mayoclinic.org/zh-hans/diseases-conditions/low-blood-pressure/diagnosis-treatment/drc-20355470'),
( 2, '藥物與治療', '低血壓 - 藥物與治療教學', 'https://www.youtube.com/watch?v=8ld19IE2Y08', 'https://www.mayoclinic.org/zh-hans/diseases-conditions/low-blood-pressure/diagnosis-treatment/drc-20355470'),
( 2, '統計與案例', '低血壓 - 統計與案例教學', 'https://www.youtube.com/watch?v=8ld19IE2Y08', 'https://health.tvbs.com.tw/medical/354288'),
( 2, '醫師講解', '低血壓 - 醫師講解教學', 'https://www.youtube.com/watch?v=8CFtJlbZdqw', 'https://helloyishi.com.tw/heart-health/hypertension/hypotension/'),
( 2, '心理調適', '低血壓 - 心理調適教學', 'https://www.youtube.com/watch?v=RJbQpe7PeKY', 'https://www.ihealth.com.tw/article/%E5%A3%93%E5%8A%9B%E8%88%87%E7%96%BE%E7%97%85/'),
( 2, '銀髮族專區', '低血壓 - 銀髮族專區教學', 'https://www.youtube.com/watch?v=iYCp_Kvf9o8', 'https://health.ltn.com.tw/article/paper/1270162'),

-- 高脈搏
(3, '預防', '高脈搏 - 預防教學', 'https://www.youtube.com/watch?v=oDVAaum-i0Y', 'https://ihealth.vghtpe.gov.tw/media/781'),
(3, '症狀', '高脈搏 - 症狀教學', 'https://www.youtube.com/watch?v=lIL-ODk7u-0', 'https://wwwv.tsgh.ndmctsgh.edu.tw/unit/10012/12861'),
(3, '飲食', '高脈搏 - 飲食教學', 'https://www.youtube.com/watch?v=U2zXjrujJ0I', 'https://www.hch.gov.tw/?aid=626&iid=689&page_name=detail&pid=62'),
(3, '改善', '高脈搏 - 改善教學', 'https://www.youtube.com/watch?v=mHtPWxOboqM', 'https://www.hch.gov.tw/?aid=626&iid=296&page_name=detail&pid=43'),
(3, '病因', '高脈搏 - 病因教學', 'https://www.youtube.com/watch?v=SDh54sluLng', 'https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=632&pid=1188'),
(3, '診斷方法', '高脈搏 - 診斷方法教學', 'https://www.youtube.com/watch?v=5yAQxHgrstU', 'https://wd.vghtpe.gov.tw/cv/cv/files/Fpage.action?fid=2178&muid=3125'),
(3, '藥物與治療', '高脈搏 - 藥物與治療教學', 'https://www.youtube.com/watch?v=Kbptu3atEnA', 'https://epaper.ntuh.gov.tw/health/202205/project_3.html'),
(3, '統計與案例', '高脈搏 - 統計與案例教學', 'https://www.youtube.com/watch?v=xWjEvkv3pdg', 'https://gec.hwai.edu.tw/app/index.php?Action=downloadfile&file=WVhSMFlXTm9Memt4TDNCMFlWODVNelV6WHpnNE1UUTFOekpmTWpJM01qWXVjR1Jt&fname=OO54USWXPK40QLXXLOPOOKGDKOPKA1YSYSUWUSNKUT25IGA0NKUSA0POFH54KLFCMKPOGGIHVS0441WWYWZWMONPOPB0B1GCJDYTVSMLNK30POFGCCLKYWRKICPO44XXA4NO41WTXSPKHGXSOPQKCCMKZTGDUTFGUSECPPEGQOA4ROTWYWDC21HH50B035GDXSB4VX2020ROMKXSZTRLSSTSZWYWYWDGPKDCXW50NPKKQLKKHCLKJGYWPKMOVXLK'),
(3, '醫師講解', '高脈搏 - 醫師講解教學', 'https://www.youtube.com/watch?v=SDh54sluLng', 'https://www.cmuh.cmu.edu.tw/Department/Detail?depid=54'),
(3, '心理調適', '高脈搏 - 心理調適教學', 'https://www.youtube.com/watch?v=e81ktq39wF4', 'https://apps.mentalwe.com/'),
(3, '銀髮族專區', '高脈搏 - 銀髮族專區教學', 'https://www.youtube.com/watch?v=bBh-zmeWKcA', 'https://www.hch.gov.tw/?aid=626&iid=715&page_name=detail&pid=62'),

-- 低脈搏
( 4, '預防', '低脈搏 - 預防教學', 'https://www.youtube.com/watch?v=Nk0MDTZ5Isc', 'https://helloyishi.com.tw/heart-health/arrhythmias/how-to-take-care-bradycardia/'),
( 4, '症狀', '低脈搏 - 症狀教學', 'https://www.youtube.com/watch?v=Eg19OcUV1i8', 'https://www.hkcardiaccentre.com/article/bradycardia'),
( 4, '飲食', '低脈搏 - 飲食教學', 'https://www.youtube.com/watch?v=7OfLcct6GJo', 'https://health.udn.com/health/story/5977/7534029'),
( 4, '改善', '低脈搏 - 改善教學', 'https://www.youtube.com/watch?v=Nk0MDTZ5Isc', 'https://helloyishi.com.tw/heart-health/arrhythmias/how-to-take-care-bradycardia/'),
( 4, '病因', '低脈搏 - 病因教學', 'https://www.youtube.com/watch?v=8CFtJlbZdqw', 'https://www.uho.com.tw/article-60216.html'),
( 4, '診斷方法', '低脈搏 - 診斷方法教學', 'https://www.youtube.com/watch?v=dlQIKO0AGf8', 'https://www.hkcardiaccentre.com/article/bradycardia'),
( 4, '藥物與治療', '低脈搏 - 藥物與治療教學', 'https://www.youtube.com/watch?v=8ld19IE2Y08', 'https://epaper.ntuh.gov.tw/health/202206/project_3.html'),
( 4, '統計與案例', '低脈搏 - 統計與案例教學', 'https://www.youtube.com/watch?v=Eg19OcUV1i8', 'https://www.hkcardiaccentre.com/article/bradycardia'),
( 4, '醫師講解', '低脈搏 - 醫師講解教學', 'https://www.youtube.com/watch?v=8CFtJlbZdqw', 'https://www.commonhealth.com.tw/article/85111'),
( 4, '心理調適', '低脈搏 - 心理調適教學', 'https://www.youtube.com/watch?v=e81ktq39wF4', 'https://heart2heart.mingpao.com/%E5%A3%93%E5%8A%9B%E3%80%81%E6%83%85%E7%B7%92%E6%AE%BA%E5%82%B7%E5%8A%9B%E5%A4%A7-%E5%BC%95%E8%87%B4%E5%BF%83%E5%BE%8B%E4%B8%8D%E6%AD%A3-%E8%AD%B7%E5%BF%83tips%EF%BC%9A%E7%AE%A1%E7%90%86%E5%A5%BD/'),
( 4, '銀髮族專區', '低脈搏 - 銀髮族專區教學', 'https://www.youtube.com/watch?v=bBh-zmeWKcA', 'https://helloyishi.com.tw/senior-healthcare/active-aging/how-to-improve-five-common-symptoms-in-elderly/'),

-- 體重過高
( 5, '預防', '體重過高 - 預防教學', 'https://www.youtube.com/watch?v=N1GRVrQHDCQ', ' https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=542&pid=708'),
( 5, '症狀', '體重過高 - 症狀教學', 'https://www.youtube.com/watch?v=BaFIqIQZDAs', ' https://www.careonline.com.tw/2023/02/ideal-body-weight.html'),
( 5, '飲食', '體重過高 - 飲食教學', 'https://www.youtube.com/playlist?list=PLaRY1VzzkEYJU0LpKfECsz8XqzG2xVzhj', ' https://www.wanfang.gov.tw/departments/education/post/a21f55edff1b067d'),
( 5, '改善', '體重過高 - 改善教學', 'https://www.youtube.com/watch?v=o5KT8ai1jo4', 'https://www.mohw.gov.tw/cp-3796-42429-1.html'),
( 5, '病因', '體重過高 - 病因教學', 'https://www.youtube.com/watch?v=SnLJIW3r4m8', ' https://www.hpa.gov.tw/Pages/List.aspx?nodeid=1757'),
( 5, '診斷方法', '體重過高 - 診斷方法教學', 'https://www.youtube.com/watch?v=RUxLIfMKDrM', 'https://health99.hpa.gov.tw/onlineQuiz/bmi'),
( 5, '藥物與治療', '體重過高 - 藥物與治療教學', 'https://www.youtube.com/watch?v=FLBVYoa_RLI', ' https://webapp.cgmh.org.tw/data/medic_data/B2006001.pdf'),
( 5, '統計與案例', '體重過高 - 統計與案例教學', 'https://www.youtube.com/watch?v=iOl3NqopejA', 'https://www.gender.ey.gov.tw/gecdb/Stat_Statistics_DetailData.aspx?sn=%2FmQvpHYEayTTt8pmhMjRvA%3D%3D'),
( 5, '醫師講解', '體重過高 - 醫師講解教學', 'https://www.youtube.com/watch?v=2Mnpf1FQh7k', ' https://www.careonline.com.tw/2023/02/ideal-body-weight.html​'),
( 5, '心理調適', '體重過高 - 心理調適教學', 'https://www.youtube.com/watch?v=QAhHVLSdyNo', 'https://www.mohw.gov.tw/fp-16-24997-1.html'),
( 5, '銀髮族專區', '體重過高 - 銀髮族專區教學', 'https://www.youtube.com/watch?v=SI_2RKiW_qs', 'https://www.mohw.gov.tw/cp-2645-20437-1.html'),

-- 體重過低
(6, '預防', '體重過低 - 預防教學', 'https://www.youtube.com/watch?v=J82q3hF5XSE', 'https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=542&pid=708'),
(6, '症狀', '體重過低 - 症狀教學', 'https://www.youtube.com/watch?v=aoP-gb856Io', ' https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=166&pid=706'),
(6, '飲食', '體重過低 - 飲食教學', 'https://www.youtube.com/watch?v=J82q3hF5XSE', ' https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=544&pid=728​'),
(6, '改善', '體重過低 - 改善教學', 'https://www.youtube.com/watch?v=d2fZKr8xMak', 'https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=825&pid=4554'),
(6, '病因', '體重過低 - 病因教學', 'https://www.youtube.com/watch?v=aoP-gb856Io', 'https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=166&pid=706'),
(6, '診斷方法', '體重過低 - 診斷方法教學', 'https://www.youtube.com/watch?v=BaFIqIQZDAs','https://health99.hpa.gov.tw/onlineQuiz/bmi'),
(6, '藥物與治療', '體重過低 - 藥物與治療教學', 'https://www.youtube.com/watch?v=ZJ3DOHmtfDQ',''),
(6, '統計與案例', '體重過低 - 統計與案例教學', 'https://www.youtube.com/watch?v=BaFIqIQZDAs','https://www.mohw.gov.tw/fp-16-64021-1.html'),
(6, '醫師講解', '體重過低 - 醫師講解教學', 'https://www.youtube.com/watch?v=BaFIqIQZDAs', 'https://www.careonline.com.tw/2023/02/ideal-body-weight.html'),
(6, '心理調適', '體重過低 - 心理調適教學', 'https://www.youtube.com/watch?v=BaFIqIQZDAs', 'https://www.mohw.gov.tw/fp-16-24997-1.html'),
(6, '銀髮族專區', '體重過低 - 銀髮族專區教學', 'https://www.youtube.com/watch?v=SI_2RKiW_qs', 'https://www.mohw.gov.tw/cp-2645-20437-1.html');


-- 建立病症對應表
CREATE TABLE Conditions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    condition_name VARCHAR(50) UNIQUE NOT NULL
);

-- 運動建議表（增加 source_url）
CREATE TABLE exercise_suggestions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    condition_combination VARCHAR(255) NOT NULL UNIQUE,
    exercise TEXT,
    source_url VARCHAR(500)
);

-- 建立資料表 hospitals
CREATE TABLE IF NOT EXISTS hospitals (
    id INT AUTO_INCREMENT PRIMARY KEY,           -- 醫院ID（主鍵）
    name VARCHAR(100) NOT NULL,                  -- 醫院名稱
    region VARCHAR(10) NOT NULL,                 -- 所屬地區（台北、新北...）
    latitude DECIMAL(9,6) NOT NULL,              -- 緯度
    longitude DECIMAL(9,6) NOT NULL,             -- 經度
    url VARCHAR(255) NOT NULL                    -- 掛號網址
);

-- 插入台北地區醫院
INSERT INTO hospitals (name, region, latitude, longitude, url) VALUES
('台大醫院', '台北', 25.04103815654721, 121.51920031676651, 'https://reg.ntuh.gov.tw/WebReg/WebReg/BranchIndex?vHospCode=T0'),
('台北榮總', '台北', 25.119768063024893, 121.52014934995734, 'https://www6.vghtpe.gov.tw/reg/home.do'),
('台北市立聯合醫院中興院區', '台北', 25.051254194653932, 121.50945158650693, 'https://webreg.tpech.gov.tw/RegOnline1_1.aspx?ChaId=A105&tab=1&ZCode=G&thidname=%A4%A4%BF%B3%B0%7C%B0%CF');

-- 插入新北地區醫院
INSERT INTO hospitals (name, region, latitude, longitude, url) VALUES
('亞東紀念醫院', '新北', 24.997161018397442, 121.45290468404376, 'https://www.femh.org.tw/webregs/start.aspx'),
('新北市立聯合醫院', '新北', 25.060757090445975, 121.49057250470027, 'https://www.ntch.ntpc.gov.tw/home.jsp?id=ODM='),
('輔仁大學附設醫院', '新北', 25.040334887250964, 121.43150825767125, 'https://www.hospital.fju.edu.tw/OPDSchedule#gsc.tab=0');

-- 插入桃園地區醫院
INSERT INTO hospitals (name, region, latitude, longitude, url) VALUES
('林口長庚醫院', '桃園', 25.061061706130765, 121.36792612966715, 'https://register.cgmh.org.tw/Register_WEEK/3'),
('桃園醫院', '桃園', 24.978233875412037, 121.26857313634306, 'https://tyghnetreg.tygh.mohw.gov.tw/OINetReg.WebRwd/'),
('聖保祿醫院', '桃園', 24.982238654199307, 121.31237651322459, 'https://www.sph.org.tw/registration/registration-online-home.html');

-- 插入台中地區醫院
INSERT INTO hospitals (name, region, latitude, longitude, url) VALUES
('中國醫藥大學附設醫院', '台中', 24.15732490347137, 120.68051019595156, 'https://www.cmuh.cmu.edu.tw/Service/OnlineAppointment'),
('台中榮總', '台中', 24.184638714966365, 120.60451968884078, 'https://register.vghtc.gov.tw/register/'),
('台中慈濟醫院', '台中', 24.19590515929466, 120.72157906652338, 'https://reg-prod.tzuchi-healthcare.org.tw/tchw/HIS5OpdReg/SecList_TC');

-- 插入台南地區醫院
INSERT INTO hospitals (name, region, latitude, longitude, url) VALUES
('奇美醫院', '台南', 23.020937527006318, 120.22209442504533, 'https://medhub.chimei.org.tw/webopd/yk/scheSelDoc'),
('成大醫院', '台南', 23.002367247143226, 120.21937381804318, 'https://tandem.hosp.ncku.edu.tw/Tandem/'),
('衛福部台南醫院', '台南', 22.996094563133752, 120.20887411030806, 'https://nreg.tnhosp.mohw.gov.tw/NEWREG/Department?hosp=1');

-- 插入高雄地區醫院
INSERT INTO hospitals (name, region, latitude, longitude, url) VALUES
('高雄長庚醫院', '高雄', 22.649777500545568, 120.35613282533016, 'https://register.cgmh.org.tw/Register/8'),
('高雄醫學大學附設中和紀念醫院', '高雄', 22.646211607469528, 120.30960438181957, 'https://www.kmuh.org.tw/Web/WebRegistration/Registration/Index'),
('高雄市立大同醫院', '高雄', 22.627193027759954, 120.29736901065453, 'https://register.mtth.org.tw/Register/W');

-- ⓮ 插入範例資料
INSERT INTO `Users` (`user_id`,`username`,`password`,`display_name`, `age`, `gender`, `height`, `weight`) VALUES
  (1,'jen',SHA2('jen0323',256),'黃明建','23','男','175','69'),
  (2,'chenbx',SHA2('xun0323',256),'陳柏勳','22','男','175','69'),
  (3,'guomr',SHA2('ren0323',256),'郭萌仁','21','男','175','69');

INSERT INTO `Families` (`family_name`,`created_by`) VALUES
  ('黃家人',1),('陳家人',2),('郭家人',3);

INSERT INTO `FamilyMembers` (`family_id`,`user_id`) VALUES
  (1,1),(2,2),(3,3);

INSERT INTO `BloodPressure` (`user_id`,`measure_at`,`timezone`,`systolic_mmHg`,`diastolic_mmHg`,`pulse_bpm`) VALUES
(1,'2025-06-02','Asia/Taipei',124,88,89);

INSERT INTO `AnxietyIndex` (`user_id`,`measure_at`,`score`,`suggestion`) VALUES
  (1,'2024-11-26',5,'輕度焦慮'),
  (1,'2024-11-27',10,'中度焦慮'),
  (1,'2024-11-28',14,'重度焦慮');

INSERT INTO `MedicationTypes` (`type_name`) VALUES
  ('高血壓藥'),('低血壓藥'),('高脈搏藥'),
  ('低脈搏藥'),('體重過高藥'),('體重過低藥');

-- 高血壓藥 (type_id = 1)
INSERT INTO Medications (name, type_id, dosage, frequency, ingredients, contraindications, side_effects, source_url) VALUES
('脈優錠（Norvasc）', 1, '5mg/tab', '每日一次', 'Amlodipine Besylate', '對成分過敏者禁用','頭痛、頭昏、下肢水腫、心悸、潮紅、噁心等', 'https://www.ntuh.gov.tw/ntuh/Fpage.action?muid=3247&amp;fid=3176'),
('冠達悅歐樂持續性藥效錠（Adalat OROS）', 1, '30mg/tab', '每日一次', 'Nifedipine', '對成分過敏者禁用', '頭痛、頭昏、潮紅、水腫、牙齦增生、心悸等', 'https://www.ntuh.gov.tw/ntuh/Fpage.action?muid=3247&amp;fid=3176'),
('壓平樂膠衣錠（Ateol）', 1, '100mg/tab', '每日一次', 'Atenolol', '對成分過敏者禁用', '疲倦、心跳變慢、噁心、皮疹、喘鳴等', 'https://www.ntuh.gov.tw/ntuh/Fpage.action?muid=3247&amp;fid=3176');

-- 低血壓藥 (type_id = 2)
INSERT INTO Medications (name, type_id, dosage, frequency, ingredients, contraindications, side_effects, source_url) VALUES
('鹽酸麻黃素注射液（Ephedrine Hydrochloride Injection）', 2, '40mg/1mL', '依醫囑', 'Ephedrine HCl', '甲狀腺機能亢進、心臟病者禁用','高血壓、心跳不規律、臉色蒼白、微顫、精神混亂或做噩夢等', 'https://www.ntuh.gov.tw/ntuh/Fpage.action?muid=3247&amp;fid=3176'),
('敏立舒注射液（Nitroglycerin Injection）', 2, '5mg/10mL', '依醫囑', 'Nitroglycerin','低血壓、青光眼患者禁用', '降血壓、頻脈、頭痛、頭重感', 'https://www.ntuh.gov.tw/ntuh/Fpage.action?muid=3247&fid=3176'),
('力復菲德注射劑（Norepinephrine Bitartrate / Levophed Injection）', 2, '4mg/4mL', '依需要使用', 'Norepinephrine Bitartrate', '末梢血管疾病、心律不整者禁用','高血壓、心律不整、周邊缺血、頭痛、呼吸急促等', 'https://www.ntuh.gov.tw/ntuh/Fpage.action?muid=3247&fid=3176');

-- 高脈搏藥 (type_id = 3)
INSERT INTO Medications (name, type_id, dosage, frequency, ingredients, contraindications, side_effects, source_url) VALUES
('心得安（Propranolol Tablets）', 3, '40mg', '每日 08:00', 'Propranolol', '哮喘患者禁用', '心跳過慢, 疲倦', 'https://www.mayoclinic.org/drugs-supplements/propranolol-oral-route/description/drg-20071164'),
('美托洛爾（Metoprolol Tablets）', 3, '100mg', '每日 12:00', 'Metoprolol', '哮喘患者禁用', '心跳過慢, 疲倦, 頭暈', 'https://www.mayoclinic.org/drugs-supplements/metoprolol-oral-route/description/drg-20071141'),
('維拉帕米（Verapamil Tablets）', 3, '80mg', '每日三次', 'Verapamil', '心衰竭患者禁用', '低血壓, 頭暈', 'https://www.mayoclinic.org/drugs-supplements/verapamil-oral-route/description/drg-20071728');

-- 低脈搏藥 (type_id = 4)
INSERT INTO Medications (name, type_id, dosage, frequency, ingredients, contraindications, side_effects, source_url) VALUES
('阿托品（Atropine Injection）', 4, '0.5mg', '依醫囑', 'Atropine', '青光眼患者禁用', '口乾, 心跳加快', 'https://www.webmd.com/drugs/2/drug-8614-99/atropine-ophthalmic-eye/atropine-sulfate-ophthalmic/details?utm_source=chatgpt.com'),
('異丙腎上腺素（Isoproterenol Injection)', 4, '0.2mg', '依需要使用', 'Isoproterenol', '心肌梗塞患者禁用', '心悸, 頭痛', 'https://my.clevelandclinic.org/health/drugs/23877-isoproterenol-injection?utm_source=chatgpt.com');

-- 體重過高藥 (type_id = 5)
INSERT INTO Medications (name, type_id, dosage, frequency, ingredients, contraindications, side_effects, source_url) VALUES
('減重優（Orlistat Capsules）', 5, '120mg', '餐前服用', 'Orlistat', '孕婦禁用', '胃腸不適, 油脂便', 'https://www.webmd.com/diet/obesity/alli-orlistat-weight-loss-pill'),
('利拉魯肽（Liraglutide Injection）', 5, '3mg', '每日一次', 'Liraglutide', '甲狀腺癌患者禁用', '噁心, 嘔吐, 腹瀉', 'https://www.webmd.com/drugs/2/drug-154569/liraglutide-injection/details'),
('塞馬魯肽（Semaglutide Injection）', 5, '2.4mg', '每週一次', 'Semaglutide', '甲狀腺癌患者禁用', '食慾減退, 腹瀉', 'https://www.webmd.com/drugs/2/drug-181368/semaglutide-injection/details');

-- 體重過低藥 (type_id = 6)
INSERT INTO Medications (name, type_id, dosage, frequency, ingredients, contraindications, side_effects, source_url) VALUES
('多補佳（Ensure Plus）', 6, '1罐', '每日 2 次', 'Ensure Plus', '糖尿病患者慎用', '腹脹, 噁心', 'https://www.ensure.com/nutrition-products/ensure-plus/milk-chocolate'),
('安素高蛋白（Ensure High Protein）', 6, '1罐', '每日一次', 'Ensure High Protein', '糖尿病患者慎用', '腹脹, 噁心', 'https://www.ensure.com/nutrition-products/ensure-high-protein/milk-chocolate-shake'),
('安怡補體素（Anlene Supplement）', 6, '1罐', '每日一次', 'Anlene', '腎功能不全者慎用', '腹脹, 噁心', 'https://www.anlene.com/sg/en/products/powders/anlene-gold-5x');

  
-- （此處可依需求插入完整的 disease_videos 資料）
INSERT INTO Conditions (condition_name) VALUES
('高血壓'), 
('肌少症'), 
('低血壓'),
('體重過重'),
('體重過輕'),
('肌高症'), 
('脈搏太高'),
('脈搏太低');

INSERT INTO exercise_suggestions (condition_combination, exercise, source_url) VALUES
('高血壓', 
 '每週至少5天、每天30分鐘輕度到中度有氧運動，例如快走、慢跑、騎腳踏車或游泳。',
 'https://www.fyh.mohw.gov.tw/?aid=509&pid=0&page_name=detail&iid=1052'),
('低血壓', 
 '進行溫和有氧運動如散步或瑜珈，避免劇烈運動導致頭暈。',
 'https://www.healthnews.com.tw/article/62112'),
('體重過重', 
 '每週5天中強度有氧運動（如快走、游泳），配合飲食控制。',
 'https://www.drmbesuperior.com/post/肥胖患者動起來，肥胖的運動指引'),
('體重過輕', 
 '適度力量訓練與有氧運動，增加蛋白質與熱量攝取以促進體重成長。',
 'https://www.hpa.gov.tw/Pages/Detail.aspx?nodeid=825&pid=4554'),
('脈搏太高', 
 '建議進行低強度有氧運動，如散步或輕鬆游泳，並充分休息。',
 'https://www.oserio.com/cht/living/降低心率的好方法-就從運動開始.html'),
('脈搏太低', 
 '進行適度有氧與輕度阻力訓練，若有症狀則需就醫檢查。',
 'https://www.mayoclinic.org/zh-hans/healthy-lifestyle/fitness/in-depth/exercise-intensity/art-20046887?utm_source=chatgpt.com');
-- 最後檢查
SHOW TABLES;
SELECT * FROM `Users`;
SELECT * FROM `Families`;
SELECT * FROM `FamilyMembers`;
SELECT * FROM `BloodPressure`;
SELECT * FROM `AnxietyIndex`;
SELECT * FROM `MedicationTypes`;
SELECT * FROM `Medications`;
SELECT * FROM `diseases`;
SELECT * FROM `disease_videos`;
SELECT * FROM  `exercise_suggestions` ;
SELECT * FROM  `hospitals` ;
SELECT * FROM  `weight_records`;
