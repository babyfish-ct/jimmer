Here is the English translation of the file, preserving the indentation of code blocks:

:::caution  
The content of this article is only for Output DTOs, Input DTOs can freely adopt the flat scheme.
:::

For output, the `flat` mode is a controversial topic. The author disagrees with it, because

-   The association model is not complex, it is not worth the system providing the necessary conversion logic

-   This will cause the backend system and specific UI frontend to be tightly coupled, losing the universality it should have *(this is very important, will be discussed in detail later)*

The deeper reason: Data structures with native associations are normalized data, while flattened objects processed by `flat` are non-associated data.

However, you may be in a work situation like this: the frontend team you work with requires all query APIs to return flattened objects across the board, requires any attribute other than collection attributes to be processed by `flat`, and is very insistent.

Now let's explain why some frontend teams have this insistence. UI projects can be considered in two categories:

-   Projects that do not require state management

    The biggest feature of such projects is that the functional areas on the interface *(excluding non-functional areas such as top bars and sidebars)* adopt an overall switching mode *(which is more likely to happen on small-screen mobile devices)*, so each UI interface is a data island.

    At this point, the main complexity of the project lies in UI rendering. Frontend developers do not need to care about data models at all.
    
    Since they don't care, the most natural idea is to expect the data structure to be exactly the same as the interface structure. For non-collection associations, the UI is usually displayed flatly, so there is a demand that all APIs return flattened objects.

-   Projects that require state management

    The biggest feature of such projects is that there are decentralized, coexisting, and complex collaborative problems in the functional areas of the interface, so there are intricate connections between different UI components.

    At this point, the main complexity of the project lies in state management, and frontend developers must pay close attention to the structure of the data model.

    Frontend state management, to some extent, is the normalization or even relationalization of data. Large amounts of `flat` data are destructive to such projects.

If the frontend team has never experienced a UI project that requires state management, they will of course be unable to realize the importance of normalized data. At this time, they will form a habitual dependence on flat structures and refuse to accept other concepts that seem contrary to their own cognition but actually have no cost.

I mentioned earlier that excessive use of `flat` mode will cause the backend system and specific UI frontend to be highly coupled. Let me think about a scenario:

1.  The mobile UI was launched first. This UI application is very simple, each page is a data island, and no state management is required. Therefore, a large number of backend APIs are designed in a `flat` style.

2.  As the system becomes more and more complex, a mobile user-facing application is not enough. An internal management system used by staff needs to be added. If the UI logic of this system is relatively complex, and is eventually determined to be a UI project that requires state management, the `flat` style backend APIs will be very difficult to handle. Either the backend develops another set of APIs that return normalized data, or the frontend writes a lot of code to restore the data back to normalized.

It is possible to persuade by discussion, pointing out the importance of normalized data for state management, and pointing out that normalized data is not a problem for many UI libraries, such as [Antd Table.Column](https://ant.design/components/table#column) The type of the `dataIndex` attribute is `string | string[]`, where `string[]` is support for hierarchical data that will not cause any development problems.

If you don't have a say in this or can't convince the other party, then you can use the `flat` function to handle it.