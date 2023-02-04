import styled from "@emotion/styled";
import { TextField } from "@mui/material";

export const WhiteTextField = styled(TextField)({
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