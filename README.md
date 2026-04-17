# MicTestProgram

Compose + Kotlin 기반 Android 음성 인식 평가 앱입니다.

## 핵심 기능
- 테스트 시작 시 매 라운드마다 삐 소리와 함께 제시 단어 표시
- 지정 시간 동안 녹음한 음성을 네이버 CLOVA Speech Recognition(STT)로 전송
- 제시 단어/인식 단어/정오 판정 표시
- 세션 종료 시 정확도(정상 인식 횟수 / 전체 테스트 횟수 * 100) 계산
- 결과를 로컬(Room DB)에 저장하고 기록표 화면에서 확인

## 키 입력 위치
아래 파일의 두 값을 실제 키로 바꿔주세요.

- `app/src/main/java/com/example/mictestprogram/network/ClovaSpeechRecognizer.kt`
  - `apiKeyId = "여기에 키를 입력하세요"`
  - `apiKeySecret = "여기에 키를 입력하세요"`

## 참고
- 50개 시험 단어는 번호/대분류/세부분류 메타데이터와 함께 코드에 고정되어 관리됩니다.
- 최초 실행 시 마이크 권한이 필요합니다.
- 현재 기본 테스트 라운드는 50회입니다.
- 선정된 50개 단어를 순차적으로 1회씩(중복 없이) 시험합니다.
- 정답 판정 시 띄어쓰기/특수문자 보정만 적용합니다 (예: 티비켜 / 티비 켜).
