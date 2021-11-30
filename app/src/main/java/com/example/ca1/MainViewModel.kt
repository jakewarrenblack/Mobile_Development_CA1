package com.example.ca1

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.ca1.api.RetrofitInstance
import com.example.ca1.data.CocktailEntity
import com.example.ca1.data.FavouriteEntity
import com.example.ca1.data.MergedData
import com.example.ca1.data.SampleDataProvider
import com.example.ca1.model.Cocktail
import com.example.ca1.model.CocktailResponse
import com.example.plantapp.localDB.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody

// https://www.geeksforgeeks.org/viewmodel-in-android-architecture-components/
// As part of the MVVM (Model, View, ViewModel) android architecture,
// we use the ViewModel class to store and manage UI-related data
// all the UI data is stored in the ViewModel rather than in the activity
// **separation of concerns**
class MainViewModel (app: Application) : AndroidViewModel(app) {
    private val database = AppDatabase.getInstance(app)

    val json: MutableLiveData<String>
        get() = _json

    val _json: MutableLiveData<String> = MutableLiveData()


    val _favourites: MutableLiveData<MutableList<FavouriteEntity?>?> = MutableLiveData()
    var tempFavourites = _favourites.value


    val favourites: LiveData<MutableList<FavouriteEntity?>?>
        get() = _favourites

    val _currentFavourite: MutableLiveData<FavouriteEntity> = MutableLiveData()

    val currentFavourite: LiveData<FavouriteEntity>
        get() = _currentFavourite

    // setter

    // we don't expose live data directly to the activity in the MVVM model
    // we use MutableLiveData as a wrapper for our live data
    // this implies we may want to edit the MutableLiveData from the activity,
    // it can be changed at runtime
    // the underscore represents a private variable, it is only accessible to this MainViewModel.kt
    private val _cocktails: MutableLiveData<List<Cocktail>> = MutableLiveData()
    val cocktails: LiveData<List<Cocktail>>
        get() = _cocktails

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading



    fun getCocktails(searchQuery: String){
        // a coroutine function can only be called from a coroutine,
        // so we make one:
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _isLoading.postValue(true)
            val fetchedCocktails = RetrofitInstance.api.getCocktails(searchQuery).drinks
            Log.i(TAG, "Fetched cocktails: $fetchedCocktails")
            _cocktails.postValue(fetchedCocktails)

                val favourite =
                    database?.favouriteDao()?.getAll()

                _favourites.postValue(favourite)

                _isLoading.postValue(false)
            }
        }
    }

    fun saveFavourite(favouriteEntity: FavouriteEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                database?.favouriteDao()?.insertFavourite(favouriteEntity)

                // Not sure what I'm doing here,
                // Trying to get the UI to update when a save is made
                _currentFavourite.postValue(favouriteEntity)
                tempFavourites?.add(favouriteEntity)
                _favourites.postValue(tempFavourites)
            }
        }
    }

    fun removeFavourite(favouriteEntity: FavouriteEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                // Pass only an ID for this one, we're removing, not inserting an entity
                database?.favouriteDao()?.removeFavourite(favouriteEntity.id)
                _currentFavourite.postValue(null)
                tempFavourites?.remove(favouriteEntity)
                _favourites.postValue(tempFavourites)
            }
        }
    }

    fun getFavourite(favouriteId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val favourite =
                    database?.favouriteDao()?.getFavouriteById(favouriteId)

                favourite?.let {
                    _currentFavourite.postValue(it)
                    Log.i("Favourite", "Cocktail Returned from DB" + it.strDrink)
                    //exists = true;
                }
            }
        }
    }

    fun getAllFavourites(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val favourites =
                    database?.favouriteDao()?.getAll()

                favourites?.let {
                    _favourites.postValue(it)
                    //exists = true;
                }
            }
        }
    }

    fun fetchData(): MediatorLiveData<MergedData> {
        val liveDataMerger = MediatorLiveData<MergedData>()
        liveDataMerger.addSource(cocktails) {
            if (it != null) {
                liveDataMerger.value = MergedData.CocktailData(it)
            }
        }
        liveDataMerger.addSource(favourites) {
            if (it != null) {
                liveDataMerger.value = MergedData.FavouriteData(it)
            }
        }
        return liveDataMerger
    }

    fun getFullJson(searchQuery: Int?){
        viewModelScope.launch {
            RetrofitInstance.api.getCocktailsJson(searchQuery).enqueue(object:
                Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    //handle error here
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    //your raw string response
                    val stringResponse = response.body()?.string()

                    _json.postValue(stringResponse)
                }

            })
        }
    }
}