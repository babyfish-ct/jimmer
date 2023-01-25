import styled from "@emotion/styled";
import { TextField } from "@mui/material";
import { useQueryClient } from "@tanstack/react-query";
import { ChangeEvent, FC, memo, useCallback, useState } from "react";

export const GlobalTenant: FC = memo(() => {

    const [tenant, setTenant] = useState<string | undefined>(() => {
        return (window as any).__tenant as string | undefined;
    });

    const queryClient = useQueryClient();

    const onTenantChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        const v = e.target.value;
        setTenant(v);
        (window as any).__tenant = v !== "" ? v : undefined;
        queryClient.invalidateQueries();
    }, [queryClient]);

    return (
        <WhiteTextField 
        label="Global tenant" 
        size="small"
        value={tenant}
        onChange={onTenantChange}/>
    );
});

const WhiteTextField = styled(TextField)({
    '& label.Mui-focused': {
        color: 'white',
    },
    '& .MuiInput-underline:after': {
        borderBottomColor: 'white',
    },
    '& .MuiOutlinedInput-root': {
        '& fieldset': {
            borderColor: 'white'
        },
        '&:hover fieldset': {
            borderColor: 'white',
            color: 'white'
        },
        '&.Mui-focused fieldset': {
            borderColor: 'white'
        }
    },
    '& .MuiFormLabel-root': {
        color: 'white'
    },
    '& .MuiInputBase-root': {
        '& input': {
            color: 'white',
            '& :input-placeholder': {
                color: 'white'
            }
        }
    }
});