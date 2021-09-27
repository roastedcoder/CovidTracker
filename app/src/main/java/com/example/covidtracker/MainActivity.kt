package com.example.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    private lateinit var perStateDailyData: Map<Unit, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidService = retrofit.create(CovidService::class.java)

        // fetch the national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if(nationalData == null) {
                    Log.w(TAG, "Didn't receive a valid response body")
                    return
                }
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })

        //fetch the state data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if(statesData == null) {
                    Log.w(TAG, "Didn't receive a valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy{it.state}
                Log.i(TAG, "Update spinner with state names")
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })

    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        // create a spark adapter
        val adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        // update radio button to select positive cases and max time by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        // display metric with most recent data

        updateInfoForDate(dailyData.last())
    }

    private fun updateInfoForDate(covidData: CovidData) {
        tvMetricLabel.text = NumberFormat.getInstance().format(covidData.positiveIncrease)
        val outputDataFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        tvDateLabel.text = outputDataFormat.format(covidData.dateChecked)
    }
}