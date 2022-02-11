# libvlc_custom

rtsp 스트리밍을 위한 libvlc 영상 플레이어 개발

### 기능
- rtsp 영상 및 실시간 스트리밍 : 영상 length에 따른 실시간 여부 체크
- 플레이어 오버레이 : 영상 제목, menu item을 위한 toolbar, 재생 버튼, 영상 시간 text, seekbar, 전체화면을 위한 버튼 제공
- 오버레이 hide timer : 특정 조건에 따라 일정 시간뒤 해당 오버레이는 사라지는 기능 지원
- 전체화면 : 편의상 전체화면 버튼 클릭으로 인한 화면 전환시 land, port 고정.(screen Orientation을 이용하여 고정 해제 기능 추가 예정)