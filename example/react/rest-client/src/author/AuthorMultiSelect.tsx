import { Alert, CircularProgress, FormControl, InputLabel, MenuItem, OutlinedInput, Select, SelectChangeEvent } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { FC, memo, useCallback, useState } from "react";
import { api } from "../common/ApiInstance";

export const AuthorMultiSelect: FC<{
    readonly value: ReadonlyArray<number>;
    readonly onChange: (value: Array<number>) => void
}> = memo(({value, onChange}) => {

    const [id, ] = useState(() => ++idSequnece);
    
    const { isLoading, data, error } = useQuery({
        queryKey: ["simpleAuthors"],
        queryFn: () => api.authorService.findSimpleAuthors()
    });

    const onMuiChange = useCallback((e: SelectChangeEvent<number[]>) => {
        const v = e.target.value;
        if (typeof v === 'string') {
            onChange(v.split(',').map(it => parseInt(it)));
        } else {
            onChange(v);
        }
    }, [onChange]);

    if (isLoading) {
        return <div><CircularProgress/>Loading items</div>
    }
    if (error) {
        return <Alert security="error">Failed to load items</Alert>
    }
    return (
        <FormControl fullWidth>
            <InputLabel id={`author-multi-select-${id}-label`}>Authors</InputLabel>
            <Select
            multiple
            labelId={`author-multi-select-${id}-label`}
            value={value as number[]}
            label="Authors"
            onChange={onMuiChange}>
                {data && data.map(item => 
                    <MenuItem value={item.id}>{item.firstName} {item.lastName}</MenuItem>
                )}
            </Select>
        </FormControl>
    );
});

let idSequnece = 0;