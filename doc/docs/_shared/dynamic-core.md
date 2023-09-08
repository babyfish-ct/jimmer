Dynamic object may lack any property, or in other words, dynamic object are not required to have all properties set.

:::caution
In dynamic object, missing properties and properties set to null are completely different things.

-   Missing property: The value of the property of the object is **unknown**, the current business is not interested in it  

-   Property set to null: The value of the property of the object is **known**, it really is nothing

In static POJOs, the two are actually indistinguishable. What's worse, developers often intentionally or unintentionally confuse the two by taking advantage of Java's lack of null safety.

The concept of dynamic object is very important and key to understanding Jimmer!
:::