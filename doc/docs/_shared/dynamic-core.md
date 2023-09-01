Here is the English translation, preserving indentation of code blocks:

Dynamic objects may lack any attribute, or in other words, dynamic objects are not required to have all attributes set.

:::caution
In dynamic objects, missing attributes and attributes set to null are completely different things.

-   Missing attribute: The value of the attribute of the object is **unknown**, the current business is not interested in it  

-   Attribute set to null: The value of the attribute of the object is **known**, it really is nothing

In static POJOs, the two are actually indistinguishable. What's worse, developers often intentionally or unintentionally confuse the two by taking advantage of Java's lack of null safety.

The concept of dynamic objects is very important and key to understanding Jimmer!
:::