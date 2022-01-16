package com.example.fuck_main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.concurrent.schedule

@SuppressLint("StaticFieldLeak")
val db = Firebase.firestore






var aId = "0"
var tStamp = "0"
var sId = "0"






/*bus stop lat-lng array zone バス停の位置情報*/
var bus_stop_locate = arrayOfNulls<LatLng>(50)

/*bus stop name zone バス停の名前*/
var bus_stop_name = arrayOfNulls<String>(50)

/*bus locate lat-lng array zone バスの現在位置*/
var bus_locate= arrayOfNulls<LatLng>(10)

/*bus time array zone*/
var bus_time_info = arrayOfNulls<String>(50)








class MainActivity : AppCompatActivity(),OnMapReadyCallback {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback







    //Test SW TF
    var test_sw = false
    //map setting
    private lateinit var mMap: GoogleMap






    //first moving
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // test button first moving
        val getButton: ToggleButton = findViewById(R.id.toggleButton)
        var timerCallback1: TimerTask.() -> Unit = {
            fetchLatestLocation()
            reload_map(mMap)
        }


        // test button tf checking
        getButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                test_sw = true
                Timer().schedule(0, 1000, timerCallback1)
            } else if (!isChecked) {
                test_sw = false
            }
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
            }
        }
    }






    // first map drawer
    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null)
        {
            mMap = googleMap
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
        {
            return
        }
        val last_locate = fusedLocationProviderClient.lastLocation
        last_locate.addOnSuccessListener {

            //now location
            var now_pojit = LatLng(it.latitude, it.longitude)


            //drawing now location point
            mMap.addMarker(MarkerOptions().position(now_pojit).title("現在地")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuck)))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(now_pojit, 20f))
        }
        val locationRequest = createLocationRequest() ?: return
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            callback,
            null
        )
    }







    //reload map drawer
    private fun reload_map(googleMap: GoogleMap?) {
        if (googleMap != null) {
            mMap = googleMap
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
        {
            return
        }
        val ll = fusedLocationProviderClient.lastLocation
        ll.addOnSuccessListener {

            // 現在位置取得
            var now_posit = LatLng(it.latitude, it.longitude)

            //マップ上のポインター削除
            mMap.clear()

            // now position redrawing 現在位置再描画
            mMap.addMarker(MarkerOptions().position(now_posit).title("現在地")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuck)))

            // now bus position redrawing バス現在位置再描画
            for (i in 0.. (bus_locate.size)-1) {
                if (bus_locate[i] != null) {
                    mMap.addMarker(
                        MarkerOptions().position(bus_locate[i]).title("バス" + "1")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.busicon))
                    )
                }
            }

            // bus stop location & limit second バス停位置描画
            for (i in 0..(bus_stop_locate.size)-1)
            {
                if(bus_stop_locate[i]!=null&& bus_time_info!=null) {
                    mMap.addMarker(
                        MarkerOptions().position(bus_stop_locate[i])
                            .title("バス停" + bus_stop_name[i] + "まであと" + bus_time_info[i])
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop))
                    )
                }
            }
        }
    }

    private fun fetchLatestLocation() {
        val latestLocation = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                101
            )
        } else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                101
            )
        } else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                101
            )
        } else {
            latestLocation.addOnSuccessListener {

                if (test_sw) {
                    if (it != null) {



                        db.collection("bus")
                            .document("newestData")
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result
                                    if (document != null && document.data != null) {
                                        aId = document.data?.get("arrivalTimeId").toString()
                                        val geoPoint = document.getGeoPoint("locate")
                                        sId = document.data?.get("stationsId").toString()
                                        tStamp = document.data?.get("timeStamp").toString()
                                        val lat = geoPoint!!.latitude
                                        val lng = geoPoint!!.longitude
                                        bus_locate[0] = LatLng(lat, lng)
                                    }
                                }
                            }




                        db.collection("arrivalTimes")
                            .document(sId)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result

                                    for(i in 0..(bus_time_info.size)-1) {
                                        if (document != null && document.data != null) {
                                            var bus_time_name: String = "station$i"
                                            println(bus_time_name)
                                            println(document.data?.get(bus_time_name).toString())
                                            bus_time_info[i] = document.data?.get(bus_time_name).toString()
                                        }
                                    }

                                }
                            }




                        // バス停の名前を取得
                        db.collection("stationsNames")
                            .document("hagisyosen_down_800")
                            .get()
                            .addOnCompleteListener{task ->
                                if(task.isSuccessful)
                                {
                                    val document = task.result
                                    //バス停の名前を取得
                                    for(i in 0..(bus_stop_name.size)-1) {
                                        if (document != null && document.data != null) {
                                            var subname = i.toString()
                                            if(document.data?.get(subname)!=null) {
                                                bus_stop_name[i] =
                                                    document.data?.get(subname).toString()
                                            }
                                            //println(document.data?.get(subname).toString())
                                        }
                                    }
                                }
                            }




                        db.collection("stations")
                            .document("hagishosen_down_800")
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result

                                    // バス停の位置情報を再取得
                                    for (i in 0..(bus_stop_locate.size)-1) {
                                        if (document != null && document.data != null&& bus_stop_name[i]!=null) {
                                            println(bus_stop_name[i])
                                            val geoPoint = document.data?.get(bus_stop_name[i]) as List<*>
                                            if(geoPoint[1]!="") {
                                                val lat = (geoPoint[1] as GeoPoint)!!.latitude
                                                val lng = (geoPoint[1] as GeoPoint)!!.longitude
                                                bus_stop_locate[i] = LatLng(lat, lng)
                                            }
                                            //print(bus_stop_locate[i])
                                        }
                                    }

                                }
                            }




                    }
                    else if (!test_sw) {
                        println("fuck")
                    }
                    val locationRequest = createLocationRequest() ?: return@addOnSuccessListener
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        null
                    )
                }
            }

        }
    }
    private fun createLocationRequest(): LocationRequest? {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
}