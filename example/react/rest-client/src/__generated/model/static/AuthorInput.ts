import type { Gender } from '../enums';

export interface AuthorInput {
    
    readonly createdTime: string;
    
    readonly firstName: string;
    
    readonly gender: Gender;
    
    readonly id?: number;
    
    readonly lastName: string;
    
    readonly modifiedTime: string;
}