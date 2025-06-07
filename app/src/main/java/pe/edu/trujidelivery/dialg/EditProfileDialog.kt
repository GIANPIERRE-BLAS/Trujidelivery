
package pe.edu.trujidelivery.dialg

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import pe.edu.trujidelivery.R

class EditProfileDialog(
    private val currentName: String,
    private val currentEmail: String,
    private val onSave: (newName: String, newEmail: String) -> Unit
) : DialogFragment() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_profile, null)

        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)

        etName.setText(currentName)
        etEmail.setText(currentEmail)

        builder.setView(view)
            .setTitle("Editar información")
            .setPositiveButton("Guardar") { _, _ ->
                val newName = etName.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                onSave(newName, newEmail)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }
}
