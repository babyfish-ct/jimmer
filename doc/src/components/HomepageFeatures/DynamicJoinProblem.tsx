import React, { FC, memo } from "react";
import CodeBlock from '@theme/CodeBlock';
import { ViewMore } from "../ViewMore";
import { useZh } from "@site/src/util/use-zh";
import { ViewDialog } from "../ViewDialog";

export const DynamicJoinProblem: FC = memo(() => {
    const zh = useZh();
    return zh ? 
        <ViewMore buttonText='行业空白' title='Jimmer DSL弥补行业空白' variant='text'>
            {INDUSTRY_GRAP_ZH}
        </ViewMore> :
        <ViewMore buttonText='industry gap' title='Jimmer DSL fills gap of industry' variant='text'>
            {INDUSTRY_GRAP_EN}
        </ViewMore>;
});

export const DynamicJoinProblemDialog: FC<{
    readonly open: boolean,
    readonly onClose: () => void
}> = memo(props => {
    const zh = useZh();
    return zh ? 
        <ViewDialog title='Jimmer DSL弥补行业空白' {...props}>
            {INDUSTRY_GRAP_ZH}
        </ViewDialog> :
        <ViewDialog title='Jimmer DSL fills gap of industry' {...props}>
            {INDUSTRY_GRAP_EN}
        </ViewDialog>;
});

const MYBATIS_1_MAPPER =
`
@org.apache.ibatis.annotations.Mapper
public interface BookMapper {

    List<Book> findBooks(
        @Nullable String name,
        @Nullable String storeName,
        @Nullable String storeWebsite
    );
}
`;

const MYBATIS_1_XML =
`
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="somepackage.BookMapper">
    <select id="findBooks" resultType="somepackage.Book">
        select * from BOOK as book
        <!-- highlight-next-line -->
        <if test="storeName != null or storeWebsite != null"> ❶
            inner join BOOK_STORE as store
                on book.STORE_ID = store.ID
        </if>
        <where>
            <if test="name != null">
                and book.NAME = #{name}
            </if>
            <if test="storeName != null"> ❷
                and store.NAME = #{storeName}
            </if>
            <if test="storeWebsite != null">
                and store.WEBSITE = #{storeWebsite} ❸
            </if>
        </where>
    </select>
</mapper>
`;

const MYBATIS_2_XML =
`<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="somepackage.AMapper">
    <select id="findAObjects" resultType="somepackage.A">
        select distinct A.id
        from A
        <!-- highlight-start -->
        <if test="bId != null or cId != null or dId != null or eId != null">
            inner join B on A.ID = B.A_ID
        </if>
        <if test="cId != null or dId != null or eId != null">
            inner join C on B.ID = C.B_ID
        </if>
        <if test="dId != null or eId != null">
            inner join D on C.ID = D.C_ID
        </if>
        <if test="eId != null">
            inner join E on D.ID = E.D_ID
        </if>
        <!-- highlight-end -->
        <where>
            <if test="aId != null">
                and A.ID = #{aId}
            </if>
            <if test="bId != null">
                and B.ID = #{bId}
            </if>
            <if test="cId != null">
                and C.ID = #{cId}
            </if>
            <if test="dId != null">
                and D.ID = #{dId}
            </if>
            <if test="eId != null">
                and E.ID = #{eId}
            </if>
        </where>
    </select>
</mapper>`;

const INDUSTRY_GRAP_ZH =
    <>

        <p>目前其他操作SQL的技术方案，无论ORM还是非ORM，都有存在一个空白领域：只考虑到了`动态where`，没有考虑`动态join`。</p>

        <p>`动态JOIN`定义：如果某些动态查询条件针对其他表而非当前表。这意味着</p>

        <ul>
        <li>条件满足时：先通过关联属性join到其他表，再对join得到的表添加动态where条件</li>
        <li>条件不满足时：不能通过关联属性join其他表</li>
        </ul>
    
        <h1>场景1</h1>

        <p>让我们先来看第一个场景，这里用面向原生SQL的MyBatis为例</p>

        <ul>
        <li>
            <p>定义MyBatis Mapper接口</p>

            <CodeBlock language='java'>{MYBATIS_1_MAPPER}</CodeBlock>

            <p>这里，所有查询参数都可能为null，很明显，这是一个动态查询。</p>

            <p>后面两个查询参数：`storeName`和`storeWebsite`，其过滤条件并不施加在当前表`BOOK`上，而是施加在父表`BOOK_STORE上`。
            即，当这两个参数中的任何一个非null时，都会生成对`BOOK_STORE`表的JOIN，这种由参数值动态决定是否添加的表连接，在本文中被称为`动态JOIN`。</p>
        </li>
        <li>
            定义MyBatis Mapper接口
            
            <CodeBlock language='xml'>{MYBATIS_1_XML}</CodeBlock>

            <p>其中，❶就是动态JOIN。然而对开发人员而言，❷和❸才是目的，❶是为支持❷和❸而不得不做的工作，其判断条件是一种负担。</p>

            <p>也许你已经注意到了，❶的判断条件使用了`or`，这也不难理解。</p>
            
            <p><b>然而，这仅仅是最简单的两表之间动态连接，对于更深的多表连接操作而言，动态连接的复杂度会急剧上升！</b></p>
        </li>
        </ul>

        <h1>场景-2</h1>

        <p>让我们再来看第二个场景，有了前面的例子作为基础，这个例子就不同任何业务耦合了。</p>

        <CodeBlock language='xml'>{MYBATIS_2_XML}</CodeBlock>

        <p>这个例子逻辑很简单，A、B、C、D和E这五张表形成了一个JOIN链，每张表都一个动态查询条件。然而，正如你所见，动态JOIN的复杂度已经变得不可接受了。</p>
        &nbsp;
        &nbsp;
        &nbsp;
        &nbsp;
    </>;

const INDUSTRY_GRAP_EN =
    <>

        <p>Currently, other technical solutions for SQL operations, whether ORM or non-ORM, have a gap: they only consider <code>dynamic where</code> but not <code>dynamic join</code>.</p>

        <p><code>Dynamic JOIN</code> is defined as follows: if certain dynamic query condition apply to other tables rather than the current table. This means that:</p>

        <ul>
            <li>When the condition is matched: first join the other table through the associated property, and then add dynamic where condition to the joined table.</li>
            <li>When the condition is not matched: cannot join other tables through the associated property.</li>
        </ul>

        <h1>Scenario 1</h1>

        <p>Let's start by looking at the first scenario, which uses MyBatis with native SQL.</p>

        <ul>
            <li>
                <p>Define the MyBatis Mapper interface</p>

                <CodeBlock language='java'>{MYBATIS_1_MAPPER}</CodeBlock>

                <p>Here, all query parameters may be null, obviously, this is a dynamic query. </p>

                <p>The following two query parameters: `storeName` and `storeWebsite`, the filter conditions are not applied to the current table `BOOK`, but to the parent table `BOOK_STORE`.
                That is, when any of these two parameters is non-null, a JOIN to the `BOOK_STORE` table will be generated. This kind of table join that is dynamically determined by the parameter value is called `dynamic JOIN` in this article. </p>
            </li>
            <li>
                Define the MyBatis Mapper interface
            
                <CodeBlock language='xml'>{MYBATIS_1_XML}</CodeBlock>

                <p>Among them, ❶ is dynamic JOIN. However, for developers, ❷ and ❸ are the goals, ❶ is the work that must be done to support ❷ and ❸, and the judgment conditions are a burden. </p>

                <p>Perhaps you have noticed that the judgment condition of ❶ uses `or`, which is not difficult to understand. </p>
            
                <p><b>However, this is only the simplest dynamic connection between two tables. For deeper multi-table join operations, the complexity of dynamic join will rise sharply! </b></p>
        </li>
        </ul>

        <h1>Scenario-2</h1>

        <p>Let's look at the second scenario. With the previous example as the basis, this example is not coupled with any business. </p>

        <CodeBlock language='xml'>{MYBATIS_2_XML}</CodeBlock>

        <p>The logic of this example is very simple. The five tables A, B, C, D, and E form a JOIN chain, and each table has a dynamic query condition. However, as you can see, the complexity of dynamic JOINs has become unacceptable. </p>
        &nbsp;
        &nbsp;
        &nbsp;
        &nbsp;
    </>;