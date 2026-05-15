package com.reprush.app.ui.member.plan.questionnaire

import com.reprush.app.ui.member.plan.PlanGenerationViewModel

interface QuestionnaireStep {
    fun isSelectionMade(): Boolean
    fun saveSelection(viewModel: PlanGenerationViewModel)
}
