package com.ryanmolyneux.letsgooo.datastore.pairings

interface IPairings<FirstThing, SecondThing> {
    fun createPairing(firstThing: FirstThing, secondThing: SecondThing);
}