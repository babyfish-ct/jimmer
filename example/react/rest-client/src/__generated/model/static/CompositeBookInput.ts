import type { Gender } from '../enums';

export interface CompositeBookInput {
    
    readonly authors: ReadonlyArray<CompositeBookInput_TargetOf_authors>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly store?: CompositeBookInput_TargetOf_store;
}

export interface CompositeBookInput_TargetOf_authors {
    
    readonly firstName: string;
    
    readonly gender: Gender;
    
    readonly lastName: string;
}

export interface CompositeBookInput_TargetOf_store {
    
    readonly name: string;
    
    readonly website?: string;
}
