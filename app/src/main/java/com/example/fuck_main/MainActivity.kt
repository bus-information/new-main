package com.example.fuck_main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
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
import io.grpc.InternalChannelz.id
import java.util.*
import kotlin.concurrent.schedule

@SuppressLint("StaticFieldLeak")
val db = Firebase.firestore


var aId = "0"
var tStamp = "0"
var sId = "0"
var temp = 0
var staticName = ""


//bus stop lat-lng array zone バス停の位置情報
var bus_stop_locate = arrayOfNulls<LatLng>(50)

//bus stop name zone バス停の名前
var bus_stop_name = arrayOfNulls<String>(50)

//bus locate lat-lng array zone バスの現在位置
var bus_locate = arrayOfNulls<LatLng>(10)

//bus time array zone バスの到着予想時刻格納用
var bus_time_info = arrayOfNulls<String>(50)

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback


    //ベータ版特有のスイッチを管理するフラグ
    private var testSw = false

    //マップの定義
    private lateinit var mMap: GoogleMap


    //初回動作
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // ベータ版ボタンの初期動作
        val getButton: ToggleButton = findViewById(R.id.toggleButton)
        val timerCallback1: TimerTask.() -> Unit = {
            getInformation()
            reloadMap(mMap)
        }


        // フラグチェッカー
        getButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                testSw = true
                val radioGroup = findViewById<RadioGroup>(R.id.RadioGroup)
                radioGroup.visibility=View.INVISIBLE
                Timer().schedule(0, 5000, timerCallback1)
            } else if (!isChecked) {
                val radioGroup = findViewById<RadioGroup>(R.id.RadioGroup)
                radioGroup.visibility=View.VISIBLE
                bus_stop_name= arrayOfNulls(50)
                bus_locate= arrayOfNulls(50)
                bus_stop_locate= arrayOfNulls(50)
                bus_time_info= arrayOfNulls(50)

                 aId = "0"
                 tStamp = "0"
                 sId = "0"
                 temp = 0
                 staticName = ""

                testSw = false
            }
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
            }
        }
    }


    // マップ初回描画など
    override fun onMapReady(googleMap: GoogleMap?) {
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
        ) {
            return
        }
        val lastLocate = fusedLocationProviderClient.lastLocation
        lastLocate.addOnSuccessListener {

            //現在地の算出
            val nowPosit = LatLng(it.latitude, it.longitude)


            //現在地のポイントをマップ上に描画
            mMap.addMarker(
                MarkerOptions().position(nowPosit).title("現在地")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuck))
            )
            //現在地までカメラ移動
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nowPosit, 20f))
        }
        val locationRequest = createLocationRequest() ?: return
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            callback,
            null
        )
    }


    //2回目以降のマップ描画
    private fun reloadMap(googleMap: GoogleMap?) {
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
        ) {
            return
        }
        val ll = fusedLocationProviderClient.lastLocation
        ll.addOnSuccessListener {
            if(it == null){
                Toast.makeText(applicationContext, "現在位置が取得できません", Toast.LENGTH_SHORT).show()
            }else{
                // 現在位置取得
                val nowPosit = LatLng(it.latitude, it.longitude)

                //マップ上に残っているポインター削除
                mMap.clear()

                // 現在位置のポイントを再描画
                mMap.addMarker(
                    MarkerOptions().position(nowPosit).title("現在地")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuck))
                )
                if (temp == 0) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nowPosit, 20f))
                    temp++
                }
            }

            //バス現在位置のポイントを再描画
            for (i in 0 until (bus_locate.size)) {
                if (bus_locate[i] != null) {
                    mMap.addMarker(
                        MarkerOptions().position(bus_locate[i]).title("バス$i")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.busicon))
                    )
                }
            }

            //バス停位置の再描画
            for (i in 0 until (bus_stop_name.size)) {
                if (bus_stop_locate[i] != null) {
                    var tempSec = bus_time_info[i]?.toIntOrNull()
                    if (tempSec != null && tempSec > 0) {
                        mMap.addMarker(
                            MarkerOptions().position(bus_stop_locate[i] as LatLng)
                                .title("バス停" + bus_stop_name[i] + "まであと" +(tempSec/3600)+"時間"+(tempSec%3600)/60+"分"+tempSec%60+"秒")
                                //.title("バス停" + bus_stop_name[i] + "まであと"+ bus_time_info[i]+"秒")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop))
                        )
                    }
                    else{
                        mMap.addMarker(
                            MarkerOptions().position(bus_stop_locate[i] as LatLng)
                                .title("バス停" + bus_stop_name[i] + "は通り過ぎたか運行が終了しています")
                                //.title("バス停" + bus_stop_name[i] + "まであと"+ bus_time_info[i]+"秒")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.busstop))
                        )
                    }
                }
            }
        }

    }

    //サーバーからバスの位置やバス停情報などを取得する
    private fun getInformation() {
        val radioGroup = findViewById<RadioGroup>(R.id.RadioGroup)
        val id = radioGroup.checkedRadioButtonId
        val checkedRadioButton = findViewById<RadioButton>(id)
        staticName = checkedRadioButton.text as String
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

                if (testSw) {
                    if (it != null) {

                        //バスの現在位置の最新状態をサーバーから取得
                        db.collection("bus")
                            .document(staticName)
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
                                else{
                                    Log.d(TAG, "データがありません")
                                    Toast.makeText(applicationContext, "バスの現在位置のデータがありません", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d(TAG, "データを取得できませんでした:", exception)
                                Toast.makeText(applicationContext, "バスの現在位置を取得できません", Toast.LENGTH_LONG).show()
                            }
                        //バス停へのバス到着予想時刻を予測
                        db.collection("arrivalTimes")
                            .document(aId)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result

                                    for (i in 0 until (bus_time_info.size)) {
                                        if (document != null && document.data != null) {
                                            val busTimeName = bus_stop_name[i]
                                            println(busTimeName)
                                            println(document.data?.get(busTimeName).toString())
                                            bus_time_info[i] = document.data?.get(busTimeName).toString()
                                        }
                                    }

                                }
                                else{
                                    Log.d(TAG, "データがありません")
                                    Toast.makeText(applicationContext, "到着予想時刻のデータがありません", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d(TAG, "データを取得できませんでした:", exception)
                                Toast.makeText(applicationContext, "到着予想時刻を取得できませんでした", Toast.LENGTH_LONG).show()
                            }


                        // バス停の名前を取得
                        println(staticName)
                        db.collection("stationsNames")
                            .document(staticName)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result
                                    //バス停の名前を取得
                                    for (i in 0 until (bus_stop_name.size)) {
                                        if (document != null && document.data != null) {
                                            val subName = i.toString()
                                            if (document.data?.get(subName) != null) {
                                                bus_stop_name[i] =
                                                    document.data?.get(subName).toString()
                                            }
                                        }
                                    }
                                }
                                else{
                                    Log.d(TAG, "データがありません")
                                    Toast.makeText(applicationContext, "バス停名のデータがありません", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d(TAG, "データを取得できませんでした:", exception)
                                Toast.makeText(applicationContext, "バス停名が取得できません", Toast.LENGTH_LONG).show()
                            }


                        //バス停の位置情報
                        db.collection("stations")
                            .document(staticName)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result

                                    // バス停の位置情報を再取得
                                    for (i in 0 until (bus_stop_locate.size)) {
                                        if (document != null && document.data != null && bus_stop_name[i] != null) {
                                            println(bus_stop_name[i])
                                            val geoPoint =
                                                document.data?.get(bus_stop_name[i]) as List<*>
                                            if (geoPoint[1] != "") {
                                                val lat = (geoPoint[1] as GeoPoint)!!.latitude
                                                val lng = (geoPoint[1] as GeoPoint)!!.longitude
                                                bus_stop_locate[i] = LatLng(lat, lng)

                                            }
                                            else{
                                                errorFinish("geoPointNotSet")
                                            }
                                        }
                                    }

                                }
                                else{
                                    Log.d(TAG, "データがありません")
                                    Toast.makeText(applicationContext, "バス停の位置情報がありません", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d(TAG, "バス停の位置情報を取得できません", exception)
                            }


                    } else if (!testSw) {
                        println("fuck") //エラー文のつもり
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

    private fun errorFinish(errorCode : String)
    {
        Toast.makeText(applicationContext, "エラーコード $errorCode のため終了します", Toast.LENGTH_LONG).show()
        moveTaskToBack (true)
    }

    private fun createLocationRequest(): LocationRequest? {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
}