package com.example.jenmix.jen8

import java.io.Serializable

data class HealthItem(
    val title: String,
    val value: String,
    val status: String,
    val suggestion: String,
    val measuredTime: String = "",         // ✅ 加入時間戳
    val encouragement: String = ""         // ✅ 每日鼓勵語
) : Serializable

data class HealthDetail(
    val title: String,
    val description: String,
    val statusAdvice: String,
    val tip: String,
    val rangeText: String,
    val iconResId: Int
)

object HealthDetailData {
    fun getDetail(title: String): HealthDetail {
        val key = title.trim()

        return when (key) {
            "BMI" -> HealthDetail(key, "BMI 是國際通用的體重與身高比值判斷標準。", "屬於標準體重範圍。", "持續均衡飲食與運動。", "18.5 ~ 24 正常", android.R.drawable.ic_menu_info_details)
            "體脂指數", "脂肪" -> HealthDetail("體脂指數", "體脂肪是身體脂肪組織的總稱。", "體脂處於健康範圍。", "可進行間歇性快走燃脂。", "男性 10~20% / 女性 20~30%", android.R.drawable.ic_menu_manage)
            "肌肉" -> HealthDetail(key, "肌肉是推動骨骼運動的主要組織。", "肌肉量偏低需增強。", "增加蛋白質攝取與阻力訓練。", ">=51% 為標準", android.R.drawable.ic_menu_add)
            "水分" -> HealthDetail(key, "身體水分佔體重大比例，維持生理功能。", "水分足夠。", "每日飲水2000c.c.以上。", "55~65% 為佳", android.R.drawable.ic_menu_gallery)
            "蛋白質" -> HealthDetail(key, "蛋白質是建構肌肉與細胞的營養素。", "蛋白質足夠。", "可適量補充優質蛋白如豆魚蛋肉。", "約 16~20% 體重", android.R.drawable.ic_menu_upload)
            "內臟脂肪" -> HealthDetail(key, "包覆於內臟周圍的脂肪。", "屬正常範圍。", "避免高糖與油炸食物。", "<10 為正常", android.R.drawable.ic_menu_sort_by_size)
            "脂肪重量" -> HealthDetail(key, "脂肪重量為脂肪率與體重乘積。", "控制在合理脂肪比下即良好。", "搭配飲食控制與運動消脂。", "約 15% 體重以內", android.R.drawable.ic_menu_week)
            "骨量" -> HealthDetail(key, "骨量反映骨骼礦物質含量。", "骨量正常。", "注意鈣質與維生素D攝取。", "3.0~5.0 公斤", android.R.drawable.ic_menu_day)
            "基礎代謝率" -> HealthDetail(key, "維持靜息狀態所需的能量。", "基礎代謝良好。", "提升代謝可增加活動量。", ">1600 男性參考", android.R.drawable.ic_menu_rotate)
            "身體年齡" -> HealthDetail(key, "推估身體功能對應的年齡值。", "與實際年齡相符，狀態不錯。", "保持規律作息與運動。", "數值低於實齡為佳", android.R.drawable.ic_menu_my_calendar)
            "體型" -> HealthDetail(key, "由 BMI 與體脂組合得出型態。", "屬健康體型。", "可依體型制訂訓練目標。", "常見如：標準型、隱性肥胖型", android.R.drawable.ic_menu_crop)
            "肥胖級別" -> HealthDetail(key, "以 BMI 判定的肥胖分類。", "尚未達到肥胖標準。", "避免攝取高熱量食物。", "0~4 級 (不肥胖~肥胖)", android.R.drawable.ic_menu_revert)
            "正常體重" -> HealthDetail(key, "依身高換算出的標準體重範圍。", "目前體重在正常值內。", "依標準體重作為健康參考目標。", "BMI 18.5~24 換算值", android.R.drawable.ic_menu_compass)
            "去脂體重" -> HealthDetail(key, "扣除脂肪後的身體其他成分總重。", "反映肌肉骨骼等組織總量。", "維持去脂體重可穩定代謝。", "越高表示體態較精實", android.R.drawable.ic_menu_agenda)
            else -> HealthDetail(key, "此項目說明尚未建立。", "請持續保持健康生活方式。", "記得規律運動、良好作息與飲食哦！", "建議區間將在後續補上", android.R.drawable.ic_menu_help)
        }
    }
}
