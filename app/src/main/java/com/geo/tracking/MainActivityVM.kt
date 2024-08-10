package com.geo.tracking

import androidx.lifecycle.ViewModel
import com.geo.tracking.domain.GetLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityVM @Inject constructor(
    private val getLocationUseCase: GetLocationUseCase
) : ViewModel() {
}