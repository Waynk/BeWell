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

class AgeFragment(
    private val onPrev: () -> Unit,
    private val onComplete: () -> Unit
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_age, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etAge = view.findViewById<AutoCompleteTextView>(R.id.etAge)
        val tvError = view.findViewById<TextView>(R.id.tvErrorAge)
        val btnPrev = view.findViewById<Button>(R.id.btnFinish)

        val ages = (10..100).map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ages)
        etAge.setAdapter(adapter)
        etAge.threshold = 1

        etAge.setOnClickListener { etAge.showDropDown() }

        btnPrev.setOnClickListener {
            val age = etAge.text.toString().toIntOrNull()
            if (age != null && age in 10..100) {
                UserPrefs.setAge(requireContext(), age)
                tvError.visibility = View.GONE
                onComplete()
            } else {
                tvError.text = getString(R.string.error_age_required)
                tvError.visibility = View.VISIBLE
            }
        }
    }
}