package com.jigmnnaga.presentation.common.login.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PolicyDocumentLayout(
        title = "개인정보 처리방침",
        badgeText = "PRIVACY POLICY",
        documentTitle = "개인정보를 소중하고 안전하게 다룹니다",
        documentDescription = "지금 나가 서비스는 이용자의 권리와 개인정보 보호를 최우선으로 생각합니다.",
        effectiveDate = "2026년 9월 1일",
        guideText = "아래 내용을 스크롤하여 개인정보의 수집·이용 및 보호 방법을 확인할 수 있어요.",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        PolicySection(title = "1. 개인정보의 수집 및 이용 목적") {
            PolicySectionBody(
                text = "회사는 회원 가입, 본인 확인, 약속 및 친구 기능 제공, 고객 문의 처리와 서비스 품질 개선을 위해 필요한 범위에서 개인정보를 처리합니다. 수집한 정보는 안내한 목적 외의 용도로 이용하지 않습니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "2. 수집하는 개인정보 항목") {
            PolicySectionBody(
                text = "필수 항목은 이름, 이메일 주소, 휴대폰 번호, 서비스 이용 기록과 기기 정보입니다. 선택 항목은 프로필 이미지와 알림 설정이며, 선택 항목에 동의하지 않아도 기본 서비스를 이용할 수 있습니다."
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF3F6EE),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "카카오 계정으로 연결한 경우 계정 식별값과 공개에 동의한 프로필 정보가 함께 제공될 수 있습니다.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 17.sp
                    ),
                    color = AppColors.MainGreen.Green700
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "3. 개인정보의 보유 및 이용 기간") {
            PolicySectionBody(
                text = "개인정보는 회원 탈퇴 또는 수집 목적 달성 시 지체 없이 파기합니다. 다만 관계 법령에 따라 보존이 필요한 결제·접속 기록은 해당 법정 기간 동안 분리하여 안전하게 보관합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "4. 개인정보의 제3자 제공") {
            PolicySectionBody(
                text = "회사는 이용자의 사전 동의 없이 개인정보를 외부에 제공하지 않습니다. 법률에 특별한 규정이 있거나 생명·안전을 위해 긴급한 경우에는 관련 절차에 따라 최소한의 정보만 제공합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "5. 이용자의 권리와 행사 방법") {
            PolicySectionBody(
                text = "이용자는 언제든 개인정보를 조회하거나 수정하고 처리 정지 및 삭제를 요청할 수 있습니다. 앱의 개인정보 설정 또는 고객센터를 통해 요청하면 본인 확인 후 신속하게 처리합니다."
            )
        }

        Spacer(Modifier.height(22.dp))
        PolicyDivider()
        Spacer(Modifier.height(22.dp))

        PolicySection(title = "6. 개인정보 보호책임자") {
            PolicySectionBody(
                text = "개인정보 관련 문의와 권리 침해 신고는 개인정보 보호책임자에게 접수할 수 있습니다. 이메일 privacy@jigeum.app으로 문의해 주세요."
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PrivacyPolicyScreenPreview() {
    JigmnnagaTheme {
        PrivacyPolicyScreen(onBackClick = {})
    }
}
