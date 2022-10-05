package ru.mephi.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.timepicker.MaterialTimePicker
import java.util.*


class DatePickerFragment: DialogFragment(){

    private val oldCalendar = GregorianCalendar()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        oldCalendar.time = DatePickerFragmentArgs.fromBundle(requireArguments()).date
        val initialHour = oldCalendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = oldCalendar.get(Calendar.MINUTE)

        val timeListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            oldCalendar.set(Calendar.HOUR_OF_DAY, hour)
            oldCalendar.set(Calendar.MINUTE, minute)
            findNavController().previousBackStackEntry?.savedStateHandle
                ?.set("key", oldCalendar.time)
        }

        return TimePickerDialog(requireContext(), timeListener, initialHour, initialMinute, true)
    }
    /*companion object {
        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }*/
}