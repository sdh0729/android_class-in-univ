package org.techtown.diary

import android.content.Context
import android.location.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.pedro.library.AutoPermissionsListener
import com.stanfy.gsonxml.GsonXmlBuilder
import com.stanfy.gsonxml.XmlParserCreator
import kotlinx.android.synthetic.main.activity_main.*
import org.techtown.diary.data.WeatherItem
import org.techtown.diary.data.WeatherResult
import org.xmlpull.v1.XmlPullParserFactory
import java.util.*

class MainActivity : AppCompatActivity(), OnTabItemSelectedListener, OnRequestListener,
    AutoPermissionsListener, MyApplication.OnResponseListener{
    companion object{ private final val TAG = "MainActivity"}

    private lateinit var fragment1 : Fragment
    private lateinit var fragment2 : Fragment
    private lateinit var fragment3 : Fragment

    lateinit var currentLocation : Location // 현재 위치
    lateinit var gpsListener : GPSListener // 위치 정보 수신

    var locationCount = 0 // 위치 정보 확인 횟수
    lateinit var currentWeather : String // 현재 날씨
    lateinit var currentAddress : String // 현재 주소
    lateinit var currentDateString : String // 현재 날짜(String)
    lateinit var currentDate : Date // 현재 날짜

    override fun onCreate(savedInstanceState: Bundle?) {
        fragment1 = Fragment1()
        fragment2 = Fragment2()
        fragment3 = Fragment3()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().replace(R.id.container,fragment1).commit() // 처음 시작시 프래그먼트 1 표시

        bottom_navigation.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.tab1->{
                    changeFragment(fragment1)
                }
                R.id.tab2->{
                    changeFragment(fragment2)
                }
                R.id.tab3->{
                    changeFragment(fragment3)
                }
            }
            true
        } // 전달받은 Id에 따른 네비게이션뷰 프래그먼트 설정
    }

    private fun changeFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.container,fragment).commit()
    } // 각 선택된 프래그먼트로 변경

    override fun onTabSelected(position: Int) {
        when(position){
            0->bottom_navigation.setSelectedItemId(R.id.tab1)
            1->bottom_navigation.setSelectedItemId(R.id.tab2)
            2->bottom_navigation.setSelectedItemId(R.id.tab3)
        }
    } // 탭 선택시 화면 변경 (Id로 전달)

    override fun onDenied(requestCode: Int, permissions: Array<String>) {
    }

    override fun onGranted(requestCode: Int, permissions: Array<String>) {
    }

    override fun onRequest(command : String){
       if (command!=null){
            if(command == "getCurrentLocation"){
                getCurrentLocation()
            }
       }
    } // fragment2에서 호출, 위치 확인 시작

    fun getCurrentLocation(){
        currentDate = Date()
        currentDateString = AppConstants.dateFormat3.format(currentDate)
        fragment2.setDateString(currentDateString)

        var manager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try{
            currentLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)!!
            val latitude = currentLocation.latitude
            val longitude = currentLocation.longitude
            val message =
                "Last Location -> Latitude : $latitude\nLongitude:$longitude"
            println(message)

            getCurrentWeather()
            getCurrentAddress()

            gpsListener = GPSListener()
            val minTime: Long = 10000
            val minDistance = 0f

            manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            minTime, minDistance, gpsListener
        )
        println("Current location requested.")

    } catch(e : SecurityException)
        {
            e.printStackTrace()
        }
    } // 위치 설정

    fun stopLocationService() {
        val manager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            manager.removeUpdates(gpsListener)
            println("Current location requested.")
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    } // 중지

    inner class GPSListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            locationCount++
            val latitude = location.latitude
            val longitude = location.longitude
            val message =
                "Current Location -> Latitude : $latitude\nLongitude:$longitude"
            println(message)
            getCurrentWeather()
            getCurrentAddress()
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }

    fun getCurrentAddress() {
        val geocoder = Geocoder(this, Locale.getDefault())
        var addresses: List<Address>? = null
        try {
            addresses = geocoder.getFromLocation(
                currentLocation.latitude,
                currentLocation.longitude,
                1
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (addresses != null && addresses.size > 0) {
            val address = addresses[0]
            currentAddress = address.locality + " " + address.subLocality
            val adminArea = address.adminArea
            val country = address.countryName
            println("Address : $country $adminArea $currentAddress")
            if (fragment2 != null) {
                fragment2.setAddress(currentAddress)
            }
        }
    } // 현재 주소

    fun getCurrentWeather() {
        val gridMap =
            GridUtil.getGrid(currentLocation.latitude, currentLocation.longitude)!!
        val gridX: Double = gridMap["x"] ?: error("")
        val gridY: Double = gridMap["y"] ?: error("")
        println("x -> $gridX, y -> $gridY")

        sendLocalWeatherReq(gridX, gridY)
    } // 현재 날씨

    fun sendLocalWeatherReq(gridX: Double, gridY: Double) {
        var url = "http://www.kma.go.kr/wid/queryDFS.jsp"
        url += "?gridx=" + Math.round(gridX)
        url += "&gridy=" + Math.round(gridY)
        val params: Map<String, String> =
            HashMap()
        MyApplication.send(
            AppConstants.REQ_WEATHER_BY_GRID,
            Request.Method.GET,
            url,
            params,
            this
        )
    }


    override fun processResponse(
        requestCode: Int,
        responseCode: Int,
        response: String?
    ) {
        if (responseCode == 200) {
            if (requestCode == AppConstants.REQ_WEATHER_BY_GRID) {
                // Grid 좌표를 이용한 날씨 정보 처리 응답
                //println("response -> " + response);
                val parserCreator = XmlParserCreator {
                    try {
                        return@XmlParserCreator XmlPullParserFactory.newInstance().newPullParser()
                    } catch (e: java.lang.Exception) {
                        throw RuntimeException(e)
                    }
                }
                val gsonXml = GsonXmlBuilder()
                    .setXmlParserCreator(parserCreator)
                    .setSameNameLists(true)
                    .create()
                val weather: WeatherResult =
                    gsonXml.fromXml<WeatherResult>(response, WeatherResult::class.java)

                // 현재 기준 시간
                try {
                    val tmDate: Date = AppConstants.dateFormat.parse(weather.header.tm)
                    val tmDateText: String = AppConstants.dateFormat2.format(tmDate)
                    println("기준 시간 : $tmDateText")
                    for (i in 0 until weather.body?.datas?.size!!) {
                        val item: WeatherItem = weather.body?.datas!![i]
                        println("#" + i + " 시간 : " + item.hour + "시, " + item.day + "일째")
                        println("  날씨 : " + item.wfKor)
                        println("  기온 : " + item.temp.toString() + " C")
                        println("  강수확률 : " + item.pop.toString() + "%")
                        println("debug 1 : " + Math.round(item.ws * 10).toInt())

//                        println("debug 1 : " + (int)Math.round(item.ws * 10));
//                        val ws: Float = java.lang.Float.valueOf(
//                            Math.round(item.ws * 10) as Int.toString
//                            ()
//                        ) / 10.0f
//                        println("  풍속 : $ws m/s")
                    }

                    // set current weather
                    val item: WeatherItem = weather.body?.datas!![0]
                    currentWeather = item.wfKor!!
                    if (fragment2 != null) {
                        fragment2.setWeather(item.wfKor)
                    }

                    // stop request location service after 2 times
                    if (locationCount > 1) {
                        stopLocationService()
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                // Unknown request code
                println("Unknown request code : $requestCode")
            }
        } else {
            println("Failure response code : $responseCode")
        }
    }

    private fun println(data: String) {
        Log.d(TAG, data)
    }
}
