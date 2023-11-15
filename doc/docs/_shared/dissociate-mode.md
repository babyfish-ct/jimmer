There are 5 modes for dissociating child objects 

<table>
<thead>
<tr>
<th>Name</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>

NONE
*(default)*

</td>
<td>

Depend on the global configuration [jimmer.default-dissociate-action-checking](/docs/configuration/dissociate-action-checking). 
    
-   If [jimmer.default-dissociate-action-checking](/docs/configuration/dissociate-action-checking) is true *(default)* or the foreign key underlying the current association is real *(there is a corresponding foreign key constraint in the database, please refer to [Real and Fake Foreign Keys](/docs/mapping/base/foreignkey))*, 
    it is treated as CHECK. 
    
-   If [jimmer.default-dissociate-action-checking](/docs/configuration/dissociate-action-checking) is false and the foreign key underlying the current association is fake *(there is no corresponding foreign key constraint in the database, please refer to [Real and Fake Foreign Keys](/docs/mapping/base/foreignkey))*, 

    it is treated as LAX. 

</td>
</tr>
<tr>
<td>LAX</td>
<td>

This option is only valid for pseudo foreign keys *(please refer to [Real and Fake Foreign Keys](/docs/mapping/base/foreignkey))*, 
otherwise it will be ignored, the same as CHECK. 

Dissociation operations are supported even if there are child objects. 
Even if the parent object is deleted *(dissociation mode is also adopted by delete commands)*, 
dangling pseudo foreign keys of child objects are allowed 
*(even if pseudo foreign keys are left dangling, the query system can still work normally)*. 

</td>
</tr>
<tr>
<td>CHECK</td>
<td>If there are child objects, disassociation is not supported, the operation is prevented by throwing an exception.</td>
</tr>
<tr>
<td>SET_NULL</td>
<td>
Set the foreign key of the disassociated child object to null. 
The prerequisite is that the many-to-one associated propety of the child object is nullable; otherwise, attempting this configuration will lead to an exception. 
</td>
</tr>
<tr>
<td>DELETE</td>
<td>Delete the disassociated child objects.</td>
</tr>
</tbody>
</table>
