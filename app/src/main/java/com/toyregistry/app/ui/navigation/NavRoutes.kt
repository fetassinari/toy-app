package com.toyregistry.app.ui.navigation

object NavRoutes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val ADD_TOY = "add_toy"
    const val EDIT_TOY = "edit_toy/{toyId}"
    const val CATEGORIES = "categories"

    fun editToy(toyId: String) = "edit_toy/$toyId"
}
