import type { Gender } from '../enums';

export interface AuthorSpecification {
    
    readonly firstName?: string;
    
    readonly gender?: Gender;
    
    readonly lastName?: string;
    
    readonly maxCreatedTime?: string;
    
    readonly minCreatedTime?: string;
}
