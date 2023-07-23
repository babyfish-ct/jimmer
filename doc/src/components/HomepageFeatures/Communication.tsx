import React, { FC, memo } from "react";
import { ViewMore } from "../ViewMore";
import Admonition from "@theme/Admonition";
import CodeBlock from "@theme/CodeBlock";
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";
import { useZh } from "@site/src/util/use-zh";

export const Communication: FC = memo(() => {
    const zh = useZh();
    return zh ? ZH : ZH;
});

const JAVA = 
`
@RestController
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public List<
        @FetchBy("SIMPLE_FETCHER") Book ❶
    > findBooks() {
        return bookRepository.findAll(
            SIMPLE_FETCHER ❷
        );
    }

    @GetMapping("book/{id}/detail")
    @Nullable
    public 
    @FetchBy("COMPLEX_FETCHER") Book ❸
    findBookById(
            @PathVariable("id") long id
    ) {
        return bookRepository.findNullable(
            id, 
            COMPLEX_FETCHER ❹
        );
    }

    private static final Fetcher<Book> SIMPLE_FETCHER = ❺
        BookFetcher.$
            .name();

    private static final Fetcher<Book> COMPLEX_FETCHER = ❻
        BookFetcher.$
            .allScalarFields()
            .store(
                BookStoreFetcher.$
                    .allScalarFields()
                    .avgPrice()
            )
            .authors(
                AuthorFetcher.$
                    .allScalarFields()
            );
}
`;

const KOTLIN = 
`
@GetMapping("/books")
fun findBooks(): List<
    @FetchBy("SIMPLE_FETCHER") Book ❶
> =
    bookRepository.findAll(
        SIMPLE_FETCHER ❷
    )

@GetMapping("/book/{id}/detail")
fun findBookById(
    @PathVariable id: Long,
): @FetchBy("COMPLEX_FETCHER") Book? = ❸
    bookRepository.findNullable(
        id, 
        COMPLEX_FETCHER ❹
    )

companion object {

    private val SIMPLE_FETCHER = ❺
        newFetcher(Book::class).by {
            name()
        }

    private val COMPLEX_FETCHER = ❻
        newFetcher(Book::class).by {

            allScalarFields()

            store {
                allScalarFields()
                avgPrice()
            }
            authors {
                allScalarFields()
            }
        }
}
`;

const TS_ZH =
`
export type BookDto = {
    // highlight-next-line
    'BookController/SIMPLE_FETCHER': {
        readonly id: number, 
        readonly name: string
    }, 
    // highlight-next-line
    'BookController/COMPLEX_FETCHER': {
        readonly id: number, 
        readonly name: string, 
        readonly edition: number, 
        readonly price: number, 
        readonly store?: {
            readonly id: number, 
            readonly name: string, 
            readonly website?: string, 
            readonly avgPrice: number
        }, 
        readonly authors: ReadonlyArray<{
            readonly id: number, 
            readonly firstName: string, 
            readonly lastName: string, 
            readonly gender: Gender
        }>
    }
}
export class BookService {
    
    async findBooks(): Promise<
        ReadonlyArray<
            // highlight-next-line
            BookDto['BookService/SIMPLE_FETCHER']
        >
    > {
        ...省略代码...
    }

    async findBookById(
        options: { readonly id: number }
    ): Promise<
        // highlight-next-line
        BookDto['BookService/COMPLEX_FETCHER'] | 
        undefined
    > {
        ...省略代码...
    }

    ...省略其他代码...
}
`;

const ZH = 

    <ViewMore buttonText="了解更多" title="服务端和客户端使用不同编程模型的前后端对接" variant="outlined">
        <h1>服务端</h1>
        <Tabs groupId="language">
            <TabItem value="java" label="Java">
                <CodeBlock language="java">{JAVA}</CodeBlock>
            </TabItem>
            <TabItem value="kotlin" label="Kotlin">
                <CodeBlock language="kotlin">{KOTLIN}</CodeBlock>
            </TabItem>
        </Tabs>
        <ul>
            <li>❶ 对外承诺，GET <code>/books</code>返回的分页对象中的每一个Book对象的形状为静态常量SIMPLE_FETCHER所表达的形状</li>
            <li>
                ❷ 内部实现，GET <code>/books</code>内部使用静态常量SIMPLE_FETCHER查询数据
                <Admonition type="caution">
                    作为对外承诺的❶和作为内部实现的❷必须一致
                </Admonition>
            </li>
            <li>❸ 对外承诺，如果GET <code>/book/{"{id}"}/detail</code>返回非null, 其形状为静态常量COMPLEX_FETCHER所表达的形状</li>
            <li>
                ❹ 内部实现，GET <code>/book/{"{id}"}/detail</code>内部使用静态常量COMPLEX_FETCHER查询数据
                <Admonition type="caution">
                    作为对外承诺的❸和作为内部实现的❹必须一致
                </Admonition>
            </li>
            <li>
                <p>❺和❻，以静态常量的方式声明对象的形状。</p>

                <p>通过@FetchBy的修饰，Jimmer就明白每个对象对外返回的数据的具体形状了，它就可以为客户端生成代码了，包括TypeScript</p>
            </li>
        </ul>
        <Admonition type="tip">
            <ul>
                <li>
                    对于以`Book`为聚合根的的数据结构而言，不同业务场景的数据结构形状不同，
                    但无论数据结构的形状如何多样化，只需定义多样化的对象形状 <i>(例子中的❺和❻)</i> 即可。
                    对于服务端的Java或Kotlin代码而言，只需要一个用一个类型来开发代码<code>Book</code>来开发应用。
                </li>
                <li>
                    对于服务内部实现而言，因为Jimmer本身支持查询任意形状的数据结构，开发人员只需以形状定义作为额外参数实现最简单的聚合根查询即可。
                    这和`MyBatis`那种对每种业务需要的数据结构都需要进行映射 <i>(或人工查询并组合不同部分的数据)</i> 的重复劳动模式有本质不同。
                </li>
                <li>
                    对于向外暴露HTTP API而言，直接返回Jimmer动态实体即可，无需进行任何<code>DTO/VO</code>转化。
                    如果需要Jimmer生成的客户端代码，使用<code>@FetchBy</code>修饰返回类型 <i>(或其范型参数)</i> 即可。
                </li>
            </ul>
        </Admonition>
        <h1>客户端</h1>
        <CodeBlock language="ts">{TS_ZH}</CodeBlock>
        <Admonition type="tip">
            对于客户端而言，每一个业务场景的API都精确定义了返回数据的DTO类型，任何调用都能得到良好的编译时检查和流畅的IDE智能提示。
        </Admonition>
    </ViewMore>;

