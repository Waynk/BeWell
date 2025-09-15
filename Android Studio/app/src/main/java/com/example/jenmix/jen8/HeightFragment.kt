package com.example.jenmix.jen8

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.jenmix.storage.UserPrefs
import com.example.jenmix.R

class HeightFragment(
    private val onPrev: () -> Unit,
    private val onNext: () -> Unit
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_height, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etHeight = view.findViewById<AutoCompleteTextView>(R.id.etHeight)
        val tvError = view.findViewById<TextView>(R.id.tvErrorHeight)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        val heights = (100..250).map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, heights)
        etHeight.setAdapter(adapter)
        etHeight.threshold = 1
        etHeight.setOnClickListener { etHeight.showDropDown() }

        btnNext.setOnClickListener {
            val height = etHeight.text.toString().toFloatOrNull()
            if (height != null && height in 100f..250f) {
                UserPrefs.setHeight(requireContext(), height)
                tvError.visibility = View.GONE
                onNext()
            } else {
                tvError.text = getString(R.string.error_height_required)
                tvError.visibility = View.VISIBLE
            }
        }
    }
}
