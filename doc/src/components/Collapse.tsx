import React, { FC, memo, PropsWithChildren, ReactNode } from "react";
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

let accordionId = 0;

export const Collapse: FC<
    PropsWithChildren<{
        readonly title: string
    }>
> = memo(({title, children}) => {
    return (
        <Accordion id={`acccordion-${++accordionId}`}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>{<h3>{title}</h3>}</AccordionSummary>
            <AccordionDetails>{children}</AccordionDetails>
        </Accordion>
    );
});