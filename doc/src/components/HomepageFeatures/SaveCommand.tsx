import React, { FC, ReactNode, memo } from "react";
import Admonition from '@theme/Admonition';
import { ViewMore } from "../ViewMore";
import { useZh } from "@site/src/util/use-zh";
import { ViewDialog } from "../ViewDialog";

export const SaveCommand: FC = memo(() => {
    const zh = useZh();
    return zh ? 
        <ViewMore buttonText="简要了解" title="保存任意形状的数据结构" variant="outlined">
            {ZH}
        </ViewMore> : 
        <ViewMore buttonText="A Brief Introduction" title="Save data structure of any shape" variant="outlined">
            {EN}
        </ViewMore>;
});

export const SaveCommandDialog: FC<{
    readonly open: boolean,
    readonly onClose: () => void
}> = memo((props) => {
    const zh = useZh();
    return zh ? 
        <ViewDialog title="保存任意形状的数据结构" {...props}>
            {ZH}
        </ViewDialog> : 
        <ViewDialog title="Save data structure of any shape" {...props}>
            {EN}
        </ViewDialog>;
});

const Save = require("@site/static/img/save.png").default;

const ZH: ReactNode = 
    <>
        <img src={Save}/>
        <ul>
            <li>
                <p>
                    <b>右上角: </b>用户传入一个任意形状的数据结构，让Jimmer写入数据库。 
                </p>
                <p>
                    这和其他ORM框架的save方法之间存在本质差异。
                    以JPA/Hibernate为例，对象的普通属性是否需要被保存通过
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/Column.html#insertable--">Column.insertable</a>和
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/Column.html#updatable--">Column.updatable</a>控制，
                    关联属性是否需要被保存通过
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/OneToOne.html#cascade--">OneToOne.cascade</a>、
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/ManyToOne.html#cascade--">ManyToOne.cascade</a>、
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/OneToMany.html#cascade--">OenToMany.cascade</a>和
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/ManyToMany.html#cascade--">ManyToOne.cascade</a>控制。
                    然而，无论如何开发人员如何配置，JPA/Hibernate能够为你保存的数据结构的形状是固定的。
                </p>
                <p>
                    Jimmer采用完全不同方法，被保存的Jimmer对象虽然是强类型的，但具备动态性 <i>(即, 不设置对象属性和把对象对象属性设置为null是完全同的两码事)</i>，
                    被设置的属性会被保存，而未被设置的属性会被忽略，这样，就可以保存任意形状的数据结构。
                </p>
            </li>
            <li>
                <p>
                    <b>左上角: </b>从数据库中查询已有的数据结构，用于和用户传入的新数据结构对比。
                </p>
                <p>
                    用户传入什么形状的数据结构，就从数据查询什么形状的数据结构，新旧数据结构的形状完全一致。所以，查询成本和对比成本由用户传入的数据结构的复杂度决定。
                </p>
            </li>
            <li>
                <p>
                    <b>下方: </b>对比新旧数据结构，找出DIFF并执行相应的SQL操作，让新旧数据一致：
                </p>
                <ul>
                    <li>
                        <span style={{color:"orange"}}>橙色部分</span>：对于在新旧数据结构中存在的实体对象，如果某些标量属性发生变化，修改数据
                    </li>
                    <li>
                        <span style={{color:"blue"}}>蓝色部分</span>：对于在新旧数据结构中存在的实体对象，如果某些关联发生变化，修改关联
                    </li>
                    <li>
                        <span style={{color:"green"}}>绿色部分</span>：对于在新数据结构中存在但在旧数据结构中不存在实体对象，插入数据并建立关联
                    </li>
                    <li>
                        <span style={{color:"red"}}>红色部分</span>：对于在旧数据结构中存在但在新数据结构中不存在实体对象，对此对象进行脱钩，清除关联并有可能删除数据
                    </li>
                </ul>
            </li>
        </ul>
        <Admonition type='tip'>
            <p>此功能的目的：把任意形状的数据结构作为一个整体，使用一行代码写入数据库，无论中间细节多复杂，都不用关心。</p>
            <p>如果你了解Web领域的<a href="https://react.dev/">React</a>或<a href="https://vuejs.org/">Vue</a>，不难看出这个功能很像`Virtual DOM diff`。</p>
        </Admonition>
        &nbsp;
        &nbsp;
        &nbsp;
    </>;

const EN =
    <>
        <img src={Save}/>
        <ul>
            <li>
                <p>
                    <b>Upper right corner: </b> The user passes in a data structure of any shape, and asks Jimmer to write it into the database.
                </p>
                <p>
                    There is an essential difference between this and the save method of other ORM frameworks.
                    Taking JPA/Hibernate as an example, whether the scalar properties of the entity need to be saved is controlled by
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/Column.html#insertable--">Column.insertable</a> and
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/Column.html#updatable--">Column.updatable</a> control,
                    and whether association properties need to be saved is controlled by
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/OneToOne.html#cascade--">OneToOne.cascade</a>,
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/ManyToOne.html#cascade--">ManyToOne.cascade</a>,
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/OneToMany.html#cascade--">OenToMany.cascade</a> and
                    <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/ManyToMany.html#cascade--">ManyToOne.cascade</a> control.
                    However, no matter how the developer configures it, the shape of the data structure that JPA/Hibernate can save for you is fixed.
                </p>
                <p>
                    Jimmer adopts a completely different approach. Although the saved jimmer object is strongly typed, it is dynamic <i>(that is, not setting the object property and setting the object property to null are completely different things)</i>,
                    Properties that are set are saved and properties that are not set are ignored, so that data structures of any shapes can be saved.
                </p>
            </li>
            <li>
                <p>
                    <b>Upper left corner: </b>Query the existing data structure from the database for comparison with the new data structure specified    by the user.
                </p>
                <p>
                    The shape of the data structure queried from database is same with the shape of new data structure give by user. Therefore, the query cost and comparison cost are determined by the complexity of the data structure specified    by the user.
                </p>
            </li>
            <li>
                <p>
                    <b>Below: </b> Compare the old and new data structures, find <code>DIFF</code> and execute the corresponding SQL operations:
                </p>
                <ul>
                    <li>
                        <span style={{color:"orange"}}>Orange part</span>: For entity objects that exist in both old and new data structures, if some scalar properties change, modify the data
                    </li>
                    <li>
                        <span style={{color:"blue"}}>Blue part</span>: For entity objects that exist in both old and new data structures, if some associations change, modify the association
                    </li>
                    <li>
                        <span style={{color:"green"}}>Green part</span>: For entity objects that exist in the new data structure but do not exist in the old data structure, insert data and create the association
                    </li>
                    <li>
                        <span style={{color:"red"}}>Red part</span>: For entity objects that exist in the old data structure but not in the new data structure, dissociate this object, clear the association and possibly delete the data
                    </li>
                </ul>
            </li>
        </ul>
        <Admonition type='tip'>
            <p>The purpose of this function: take the data structure of any shape as a whole, and use one line of code to write it into the database, no matter how complicated the intermediate details are, you don't have to care. </p>
            <p>If you know <a href="https://react.dev/">React</a> or <a href="https://vuejs.org/">Vue</a> in the web field, it is not difficult to see that this function is very similar to `Virtual DOM diff`. </p>
        </Admonition>
        &nbsp;
        &nbsp;
        &nbsp;
    </>;