package com.martianlab.recipes.flutter

import com.martianlab.recipes.domain.api.RoutingApi
import com.martianlab.recipes.entities.Destination


class RouterImpl : RoutingApi {
    override fun goTo(destination: Destination) {
        println("go to " + destination)
    }

    override fun goBack(){
        println("go back")
    }
}