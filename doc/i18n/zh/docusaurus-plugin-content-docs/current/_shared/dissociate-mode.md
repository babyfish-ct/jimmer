import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

子对象脱勾操作有5种模式

<table>
<thead>
<tr>
<th>模式</th>
<th>描述</th>
</tr>
</thead>
<tbody>
<tr>
<td>

NONE

(默认)

</td>
<td>

视全局配置[jimmer.default-dissociate-action-checking](/docs/configuration/dissociate-action-checking)而定

-   如果[jimmer.default-dissociate-action-checking](/docs/configuration/dissociate-action-checking)为true *(默认)* 或 当前关联所基于的外键是真的 *(数据库中存在相应的外键约束，请参见[真假外键](/docs/mapping/base/foreignkey))*，视为CHECK。

-   如果[jimmer.default-dissociate-action-checking](/docs/configuration/dissociate-action-checking)为false且当前关联所基于的外键是假的 *(数据库中没有相应的外键约束，请参见[真假外键](/docs/mapping/base/foreignkey))*，视为LAX。

</td>
</tr>
<tr>
<td>LAX</td>
<td>

该选项只对伪外键有效 *(请参见[真假外键](/docs/mapping/base/foreignkey))*，否则，会被忽略，同CHECK。

即便存在子对象，也支持脱钩操作。即使发生父对象被删除的情况 *(脱钩模式也被删除指令采用)*，也任由子对象的伪外键发生悬挂问题 *(即便伪外键发生悬挂，查询系统也可以正常工作)*。

</td>
</tr>
<tr>
<td>CHECK</td>
<td>如果存在子对象，则不支持脱勾操作，通过抛出异常阻止操作。</td>
</tr>
<tr>
<td>SET_NULL</td>
<td>把被脱勾的子对象的外键设置为null。前提是子对象的多对一关联属性是nullable的；否则尝试此配置将会导致异常。</td>
</tr>
<tr>
<td>DELETE</td>
<td>将被脱勾的子对象删除。</td>
</tr>
</tbody>
</table>
