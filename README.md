# libvlc_custom

rtsp 스트리밍을 위한 libvlc 영상 플레이어 개발

### 기능
- rtsp 영상 및 실시간 스트리밍 : 영상 length에 따른 실시간 여부 체크
- 플레이어 오버레이 : 영상 제목, menu item을 위한 toolbar, 재생 버튼, 영상 시간 text, seekbar, 전체화면을 위한 버튼 제공
- 오버레이 hide timer : 특정 조건에 따라 일정 시간뒤 해당 오버레이는 사라지는 기능 지원
- 전체화면 : 편의상 전체화면 버튼 클릭으로 인한 화면 전환시 land, port 고정.(screen Orientation을 이용하여 고정 해제 기능 추가 예정)

### 히스토리
- libvlc, mediaplayer를 싱글톤으로 관리하게 될 경우 아래와 같은 문제가 발생하였다.
  activity가 sleep에 들어가게 될 경우 일정 시간 후 간직하고 있는 service가 destroy된다.
  이때 activity를 awake하게 되면 service가 create하게 되고, 싱글톤으로 관리 중인 libvlc의 특정 값이 날아가버려 참조할 경우 exception이 발생하게 된다.
  -> service create 할때 마다 객체 생성하도록 변경 하였다.
  
- 위와 같이 만들 경우 activity내 서비스 관리, ui 터치 관리 등 fragment가 많은 것을 가지고 있게 된다. 따라서 의존적이지 않도록 다시 설계를 해야 한다.