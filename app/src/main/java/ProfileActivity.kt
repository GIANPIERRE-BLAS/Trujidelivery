package pe.edu.trujidelivery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.trujidelivery.dialg.EditProfileDialog
import pe.edu.trujidelivery.notification.NotificationUtils

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var imageView: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnChangePhoto: Button
    private lateinit var btnEditProfile: Button

    private val PICK_IMAGE_REQUEST = 100
    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        imageView = findViewById(R.id.profile_image)
        nameText = findViewById(R.id.tv_name)
        emailText = findViewById(R.id.tv_email)
        btnLogout = findViewById(R.id.btn_logout)
        btnChangePhoto = findViewById(R.id.btn_change_photo)
        btnEditProfile = findViewById(R.id.btn_edit_profile)

        val currentUser = auth.currentUser
        currentUser?.let { updateUI(it) }

        btnLogout.setOnClickListener {
            val usuarioId = auth.currentUser?.uid ?: return@setOnClickListener
            NotificationUtils.eliminarNotificacionesUsuario(this, usuarioId)
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnChangePhoto.setOnClickListener {
            abrirGaleria()
        }

        btnEditProfile.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            val dialog = EditProfileDialog(user.displayName ?: "", user.email ?: "") { newName, newEmail ->
                actualizarPerfilFirebase(newName, newEmail)
            }
            dialog.show(supportFragmentManager, "EditProfileDialog")
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            imageUri = data.data
            subirImagenABase64(imageUri!!)
        }
    }

    private fun subirImagenABase64(uri: Uri) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        convertirUriABase64(uri) { base64 ->
            if (base64 != null) {
                val data = mapOf("fotoPerfilBase64" to base64)
                db.collection("usuarios").document(uid).set(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Foto guardada", Toast.LENGTH_SHORT).show()
                        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                        Glide.with(this).load(imageBytes).circleCrop().into(imageView)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error guardando la foto", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileActivity", "Error al guardar Base64", it)
                    }
            } else {
                Toast.makeText(this, "No se pudo convertir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertirUriABase64(uri: Uri, callback: (String?) -> Unit) {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.let {
            val bytes = it.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
            callback(base64String)
        } ?: callback(null)
    }

    private fun actualizarPerfilFirebase(newName: String, newEmail: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val data = mapOf("nombre" to newName, "email" to newEmail)
        db.collection("usuarios").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show()

                val profileUpdates = userProfileChangeRequest {
                    displayName = newName
                }

                user.updateProfile(profileUpdates).addOnCompleteListener {
                    if (it.isSuccessful && user.email != newEmail) {
                        user.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) updateUI(user)
                            else Toast.makeText(this, "Error actualizando email: ${emailTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        updateUI(user)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar datos: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUI(user: FirebaseUser) {
        nameText.text = "Nombre: ${user.displayName ?: "Usuario"}"
        emailText.text = "Email: ${user.email ?: "No especificado"}"

        val uid = user.uid
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.contains("fotoPerfilBase64")) {
                    val base64String = document.getString("fotoPerfilBase64")
                    base64String?.let {
                        val imageBytes = Base64.decode(it, Base64.DEFAULT)
                        Glide.with(this).load(imageBytes).circleCrop()
                            .placeholder(R.drawable.default_profile)
                            .into(imageView)
                    }
                } else {
                    Glide.with(this).load(user.photoUrl).circleCrop()
                        .placeholder(R.drawable.default_profile)
                        .into(imageView)
                }
            }
            .addOnFailureListener {
                Glide.with(this).load(user.photoUrl).circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(imageView)
            }
    }
}