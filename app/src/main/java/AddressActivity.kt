package pe.edu.trujidelivery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pe.edu.trujidelivery.databinding.DialogAddAddressBinding
import pe.edu.trujidelivery.network.NominatimService
import pe.edu.trujidelivery.network.RetrofitClient
import pe.edu.trujidelivery.ui.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddressActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var controller: IMapController
    private lateinit var addressInput: TextInputEditText
    private lateinit var btnConfirm: MaterialButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var savedAddressesContainer: ViewGroup

    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private var currentMarker: Marker? = null

    private var userEmail: String? = null
    private var userName: String? = null
    private var esPago: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_address)

        userEmail = intent.getStringExtra("user_email")
        userName = intent.getStringExtra("name")
        esPago = intent.getBooleanExtra("es_pago", false)

        initViews()
        setupMap()
        setupListeners()
    }

    private fun initViews() {
        map = findViewById(R.id.map)
        addressInput = findViewById(R.id.addressInput)
        btnConfirm = findViewById(R.id.btnConfirm)
        savedAddressesContainer = findViewById(R.id.savedAddressesContainer)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        controller = map.controller
        controller.setZoom(18.0)
        controller.setCenter(GeoPoint(-8.1116, -79.0282))
    }

    private fun setupListeners() {
        findViewById<View>(R.id.currentLocationContainer).setOnClickListener {
            checkLocationPermission()
        }

        btnConfirm.setOnClickListener {
            confirmAddress()
        }

        findViewById<MaterialButton>(R.id.btnAddAddress).setOnClickListener {
            showAddAddressDialog()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun obtenerUbicacionActual() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val punto = GeoPoint(location.latitude, location.longitude)
                    updateMapLocation(punto)
                    obtenerDireccion(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapLocation(punto: GeoPoint) {
        controller.setCenter(punto)
        agregarMarcador(punto)
        map.invalidate()
    }

    private fun agregarMarcador(punto: GeoPoint) {
        currentMarker?.let { map.overlays.remove(it) }

        val marker = Marker(map)
        marker.position = punto
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Tu ubicación actual"
        map.overlays.add(marker)
        currentMarker = marker
    }

    private fun obtenerDireccion(lat: Double, lon: Double) {
        val call = RetrofitClient.nominatimService.getDireccion(lat, lon)
        call.enqueue(object : Callback<NominatimService.NominatimResult> {
            override fun onResponse(
                call: Call<NominatimService.NominatimResult>,
                response: Response<NominatimService.NominatimResult>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        addressInput.setText(result.display_name)
                    } else {
                        showError("No se encontró la dirección")
                    }
                } else {
                    showError("Respuesta inválida del servidor")
                }
            }

            override fun onFailure(call: Call<NominatimService.NominatimResult>, t: Throwable) {
                showError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun showAddAddressDialog() {
        val binding = DialogAddAddressBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Agregar nueva dirección")
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = binding.etAddressName.text.toString()
                val direccion = binding.etAddress.text.toString()

                if (nombre.isNotBlank() && direccion.isNotBlank()) {
                    addSavedAddress(nombre, direccion)
                } else {
                    showError("Completa todos los campos")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addSavedAddress(name: String, address: String) {
        val addressView = LayoutInflater.from(this)
            .inflate(R.layout.item_saved_address, savedAddressesContainer, false)

        addressView.findViewById<TextView>(R.id.tvAddressName).text = name
        addressView.findViewById<TextView>(R.id.tvAddress).text = address

        addressView.setOnClickListener {
            addressInput.setText(address)
        }

        savedAddressesContainer.addView(addressView)
    }

    private fun confirmAddress() {
        val direccion = addressInput.text.toString()
        if (direccion.isNotBlank()) {
            if (esPago) {
                val resultIntent = Intent().apply {
                    putExtra("direccion", direccion)
                    putExtra("user_email", userEmail)
                    putExtra("name", userName ?: "")
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("direccion", direccion)
                    putExtra("user_email", userEmail)
                    putExtra("name", userName ?: "")
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
        } else {
            showError("Por favor ingresa o selecciona una dirección")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual()
        } else {
            showError("Se necesita permiso para obtener ubicación")
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}