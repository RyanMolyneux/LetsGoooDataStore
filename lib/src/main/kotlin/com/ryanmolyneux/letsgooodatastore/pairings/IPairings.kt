package com.ryanmolyneux.letsgooodatastore.pairings

interface IPairings<FirstThing, SecondThing> {
    fun createPairing(firstThing: FirstThing, secondThing: SecondThing);
}