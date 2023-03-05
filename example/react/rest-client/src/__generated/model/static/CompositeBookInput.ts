import type { Gender } from '../enums';

export interface CompositeBookInput {
    
    readonly authors: ReadonlyArray<CompositeBookInput_AuthorTarget>;
    
    readonly edition: number;
    
    readonly id?: number;
    
    readonly name: string;
    
    readonly price: number;
    
    readonly store?: CompositeBookInput_StoreTarget;
}

export interface CompositeBookInput_AuthorTarget {
    
    readonly firstName: string;
    
    readonly gender: Gender;
    
    readonly lastName: string;
}

export interface CompositeBookInput_StoreTarget {
    
    readonly name: string;
    
    readonly website?: string;
}
