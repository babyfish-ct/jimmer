import type { TargetOf_childNodes } from './';

export interface RecursiveTreeInput {
    
    readonly childNodes?: ReadonlyArray<TargetOf_childNodes>;
    
    readonly id?: number;
    
    readonly name: string;
}