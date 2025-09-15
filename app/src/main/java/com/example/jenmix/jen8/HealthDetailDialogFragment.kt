package com.example.jenmix.jen8

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.jenmix.R

class HealthDetailDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_ITEM = "health_item"

        fun newInstance(item: HealthItem): HealthDetailDialogFragment {
            val fragment = HealthDetailDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var item: HealthItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("DEPRECATION")
            item = it.getSerializable(ARG_ITEM) as? HealthItem
        }
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return inflater.inflate(R.layout.dialog_health_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvValue = view.findViewById<TextView>(R.id.tvDetailValue)
        val tvStatus = view.findViewById<TextView>(R.id.tvDetailStatus)
        val ivStatus = view.findViewById<ImageView>(R.id.iconDetailStatus) // 新增的圖示
        val tvSuggestion = view.findViewById<TextView>(R.id.tvDetailSuggestion)
        val btnOK = view.findViewById<Button>(R.id.btnOK)

        item?.let {
            tvTitle.text = it.title
            tvValue.text = it.value

            val (colorRes, iconRes) = when (it.status) {
                "正常" -> R.color.green to R.drawable.ic_bmi_normal
                "過重" -> R.color.orange to R.drawable.ic_bmi_warning
                "肥胖" -> R.color.red to R.drawable.ic_bmi_danger
                "過輕" -> R.color.blue to R.drawable.ic_bmi_thin
                else -> R.color.gray to R.drawable.ic_bmi_unknown
            }

            tvStatus.text = it.status
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            ivStatus.setImageResource(iconRes)

            tvSuggestion.text = it.suggestion
        }

        btnOK.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(true)
        isCancelable = true
    }
}
