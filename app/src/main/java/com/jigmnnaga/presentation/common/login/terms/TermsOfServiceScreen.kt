package com.jigmnnaga.presentation.common.login.terms

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

@Composable
fun TermsOfServiceScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PolicyDocumentLayout(
        title = "이용약관",
        badgeText = "TERMS OF SERVICE",
        documentTitle = "지금 나가 서비스 이용약관",
        documentDescription = "지금 나가 서비스 이용과 관련된 이용자의 권리와 의무 및 서비스 이용 조건을 안내합니다.",
        effectiveDate = "2026년 9월 1일",
        guideText = "아래 내용을 스크롤하여 서비스 이용 조건과 이용자의 권리 및 의무를 확인할 수 있어요.",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        PolicySection(title = "제1조 - 목적") {
            PolicySectionBody(
                text = "본 약관은 지금 나가(이하 '서비스')가 제공하는 약속 생성, 일정 조율, 장소 선택, 위치 공유 및 도착 예정 시간 안내 서비스의 이용과 관련하여 서비스와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제2조 - 용어의 정의") {
            PolicySectionBody(
                text = "1. '서비스'란 이용자가 약속을 생성하고, 참여자와 일정을 조율하며, 약속 장소와 이동 정보를 확인할 수 있도록 제공되는 서비스를 말합니다.\n" +
                    "2. '회원'이란 서비스에 로그인하여 서비스를 이용하는 사람을 말합니다.\n" +
                    "3. '약속 생성자'란 약속을 생성하고 참여자를 초대하며 최종 약속을 확정하는 회원을 말합니다.\n" +
                    "4. '참여자'란 약속 생성자의 초대를 받아 약속에 참여하는 회원을 말합니다.\n" +
                    "5. '약속'이란 회원들이 날짜, 시간 및 장소를 조율하여 함께 만나기로 확정한 일정을 말합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제3조 - 회원가입 및 계정 관리") {
            PolicySectionBody(
                text = "1. 회원가입 및 로그인은 서비스에서 제공하는 카카오 로그인을 통해 진행할 수 있습니다.\n" +
                    "2. 회원은 본인의 계정을 안전하게 관리해야 하며, 타인의 계정을 부정하게 사용해서는 안 됩니다.\n" +
                    "3. 회원은 서비스 이용 시 정확한 정보를 제공해야 하며, 타인의 권리를 침해하는 방식으로 서비스를 이용해서는 안 됩니다.\n" +
                    "4. 회원은 서비스에서 제공하는 절차에 따라 탈퇴를 요청할 수 있습니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제4조 - 서비스의 제공") {
            PolicySectionBody(
                text = "1. 약속 생성 및 초대 링크 발급\n" +
                    "2. 참여자의 가능한 날짜와 시간 수집\n" +
                    "3. 약속 장소 후보 등록 및 선호도 확인\n" +
                    "4. 참여 현황 확인 및 최종 약속 확정\n" +
                    "5. 약속 관련 채팅 및 알림\n" +
                    "6. 약속 당일 출발 여부, 위치 및 도착 예정 시간 확인\n" +
                    "7. 그 밖에 약속 관리와 관련된 부가 기능\n\n" +
                    "서비스의 세부 기능은 운영 상황에 따라 추가되거나 변경될 수 있습니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제5조 - 약속 생성 및 참여") {
            PolicySectionBody(
                text = "1. 약속 생성자는 약속명, 일정 조율 기간, 날짜 및 시간 후보, 장소 후보 등을 설정하여 약속을 생성할 수 있습니다.\n" +
                    "2. 참여자는 초대 링크를 통해 약속 정보를 확인하고, 서비스에서 제공하는 절차에 따라 참여 의사를 표시할 수 있습니다.\n" +
                    "3. 참여자는 자신이 실제로 참석할 수 있는 날짜와 시간을 선택하고, 필요한 경우 장소 선호도를 제출해야 합니다.\n" +
                    "4. 약속 생성자는 참여 현황을 확인한 후 최종 날짜, 시간 및 장소를 결정할 수 있습니다.\n" +
                    "5. 약속의 최종 확정 여부는 서비스 내에 표시되는 확정 정보를 기준으로 합니다.\n" +
                    "6. 약속의 변경 또는 취소가 필요한 경우 이용자는 관련 기능을 통해 변경 사항을 공유해야 합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제6조 - 위치 공유 및 이동 정보") {
            PolicySectionBody(
                text = "1. 서비스는 약속 당일 이용자의 이동 상황을 확인할 수 있도록 위치 및 도착 예정 시간 관련 기능을 제공할 수 있습니다.\n" +
                    "2. 위치 기반 기능은 기기의 위치 권한, 네트워크 상태 및 서비스 설정 등에 따라 이용이 제한될 수 있습니다.\n" +
                    "3. 위치 공유 기능을 이용할 때에는 기기 권한 설정과 서비스에서 안내하는 동의 절차를 확인해야 합니다.\n" +
                    "4. 지도상의 위치 및 도착 예정 시간은 측정 환경, 교통 상황 및 외부 지도 서비스 등의 영향을 받을 수 있으며, 실제 위치나 도착 시간과 차이가 발생할 수 있습니다.\n" +
                    "5. 이용자는 다른 참여자의 위치 및 이동 정보를 서비스에서 안내하는 목적과 범위에 따라 이용해야 합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제7조 - 회원의 의무 및 금지 행위") {
            PolicySectionBody(
                text = "회원은 다음 행위를 해서는 안 됩니다.\n" +
                    "1. 타인의 개인정보 또는 계정을 무단으로 사용하는 행위\n" +
                    "2. 허위 정보를 이용하여 약속을 생성하거나 참여하는 행위\n" +
                    "3. 다른 회원을 괴롭히거나 불쾌감을 유발하는 행위\n" +
                    "4. 타인의 동의 없이 위치 또는 이동 정보를 수집하거나 외부에 유포하는 행위\n" +
                    "5. 서비스의 정상적인 운영을 방해하는 행위\n" +
                    "6. 관계 법령 또는 본 약관에 위반되는 행위"
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제8조 - 서비스의 변경 및 중단") {
            PolicySectionBody(
                text = "1. 서비스는 기능 개선, 시스템 점검, 장애 또는 기타 운영상 필요한 사유로 서비스의 전부 또는 일부를 변경하거나 중단할 수 있습니다.\n" +
                    "2. 서비스에 중대한 변경이나 중단이 발생하는 경우 서비스 내 공지 등 적절한 방법으로 안내합니다.\n" +
                    "3. 서비스는 이용자의 원활한 이용을 위해 합리적인 범위에서 장애를 예방하고 복구하도록 노력합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제9조 - 회원 탈퇴 및 이용 제한") {
            PolicySectionBody(
                text = "1. 회원은 서비스에서 제공하는 절차에 따라 탈퇴를 요청할 수 있습니다.\n" +
                    "2. 회원 탈퇴 시 서비스 이용이 종료되며, 관련 정보는 개인정보 처리방침 및 관계 법령에 따라 처리됩니다.\n" +
                    "3. 회원이 약관을 위반하거나 서비스 운영을 현저히 방해하는 경우 서비스는 위반 정도와 관련 절차에 따라 이용을 제한할 수 있습니다.\n" +
                    "4. 탈퇴 또는 이용 제한에 따라 약속 정보나 일부 기능을 이용할 수 없게 될 수 있습니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "제10조 - 약관의 변경 및 기타") {
            PolicySectionBody(
                text = "1. 서비스는 관계 법령을 준수하는 범위에서 본 약관을 변경할 수 있습니다.\n" +
                    "2. 약관이 변경되는 경우 변경 내용과 시행일을 서비스 내 공지 등 적절한 방법으로 안내합니다.\n" +
                    "3. 본 약관에 명시되지 않은 사항은 관계 법령 및 일반적인 거래 관행에 따릅니다.\n" +
                    "4. 서비스 이용과 관련하여 분쟁이 발생하는 경우 관계 법령에 따라 해결합니다."
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun TermsOfServiceScreenPreview() {
    JigmnnagaTheme {
        TermsOfServiceScreen(onBackClick = {})
    }
}
