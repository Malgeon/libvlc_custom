# libvlc_custom

rtsp 스트리밍을 위한 libvlc 영상 플레이어 개발

### 기능
- rtsp 영상 및 실시간 스트리밍 : 영상 length에 따른 실시간 부여 체크
- 플레이어 오버레이 : 영상 제목, menu item을 위한 toolbar, 재생 버튼, 영상 시간 text, seekbar, 전체화면을 위한 버튼 제공
- 전체화면 : 편의상 전체화면 버튼 입력시 land, port 고정 해놓았다.(screen Orientation을 이용하여 고정 해제 기능 추가 예정)

### 미지원
- 라이브러리 : 해당 프로그램은 rtsp streaming을 exo player가 지원하지 않음에 따라 만들었고, 유지 보수 또한 하지 않을 것이므로(exo 에서도 해당 기능을 지원해주길..) 라이브러리로 만들지 않았다. 