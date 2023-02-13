import type { Gender } from '../enums';

export interface AuthorInput {
    
    readonly firstName: string;
    
    readonly gender: Gender;
    
    readonly id?: number;
    
    readonly lastName: string;
}
