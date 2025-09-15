package com.example.jenmix.jen8

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.jenmix.storage.UserPrefs
import com.example.jenmix.R

class GenderFragment(
    private val onNext: () -> Unit
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gender, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnMale = view.findViewById<ImageButton>(R.id.btnMale)
        val btnFemale = view.findViewById<ImageButton>(R.id.btnFemale)
        val tvErrorGender = view.findViewById<TextView>(R.id.tvErrorGender)
        val btnNext = view.findViewById<View>(R.id.btnNext)

        btnMale.setOnClickListener {
            UserPrefs.setGender(requireContext(), getString(R.string.gender_male))
            btnMale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gender_male)
            btnFemale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray_light)
            tvErrorGender.visibility = View.GONE
        }

        btnFemale.setOnClickListener {
            UserPrefs.setGender(requireContext(), getString(R.string.gender_female))
            btnFemale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.status_warning)
            btnMale.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray_light)
            tvErrorGender.visibility = View.GONE
        }

        btnNext.setOnClickListener {
            if (UserPrefs.getGender(requireContext()).isEmpty()) {
                tvErrorGender.text = getString(R.string.error_gender_required)
                tvErrorGender.visibility = View.VISIBLE
            } else {
                onNext()
            }
        }
    }
}
