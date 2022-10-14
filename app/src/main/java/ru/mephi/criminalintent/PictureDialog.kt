package ru.mephi.criminalintent

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File
import java.util.*

class PictureDialog : DialogFragment() {

    private lateinit var photoFile: File
    private lateinit var photoView: ImageView
    private lateinit var  bmp: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoFile = PictureDialogArgs.fromBundle(requireArguments()).photoFile
        bmp = BitmapFactory.decodeFile(photoFile.path)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       Objects.requireNonNull(dialog)?.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        val view = inflater.inflate(R.layout.fragment_picture_dialog, container, false)
        photoView = view.findViewById(R.id.crimePicture)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        photoView.setImageBitmap(bmp)

    }

   /* override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = PictureDialogArgs.fromBundle(requireArguments()).photoFile
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_picture_dialog, null)
        builder.setView(view)
        val crimePhoto = view?.findViewById(R.id.crimePicture) as ImageView
        val bmp = BitmapFactory.decodeFile(photoFile.path)
        crimePhoto.setImageBitmap(bmp)
        builder.setTitle("Crime Photo")
            .setNegativeButton("Close") { _, _ ->
                dialog?.cancel()
            }
        return builder.show().apply {
            this.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) }
    }*/
}