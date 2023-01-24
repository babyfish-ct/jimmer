import type { Book } from './';
import type { Gender } from '../enums';

export interface Author {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly id: number;
    
    readonly firstName: string;
    
    readonly lastName: string;
    
    readonly gender: Gender;
    
    readonly books: ReadonlyArray<Book>;
}