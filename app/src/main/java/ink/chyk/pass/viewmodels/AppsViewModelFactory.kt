package ink.chyk.pass.viewmodels

import androidx.lifecycle.*
import com.tencent.mmkv.*
import ink.chyk.pass.api.*

class AppsViewModelFactory(
  private val onFailed: () -> Boolean
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
      val pass = PassApi(onFailed)
      val mmkv = MMKV.defaultMMKV()
      return AppsViewModel(
        pass,
        mmkv,
        CampusRun(pass, mmkv),
        IpGatewayApi(pass, mmkv)
      ) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}