package ink.chyk.pass

enum class LoadingState {
  LOADING,
  PARTIAL_SUCCESS,  // 此状态已经获得 URL
  SUCCESS,
  FAILED
}