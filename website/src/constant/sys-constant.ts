/**
 * <p>系统 常量 </p>
 * @author fxbsujay@gmail.com
 * @version 20:29 2022/6/18
 */

export interface Props {
    [key:string]:unknown
}

export class Constant {
    public key: string = ''
    public value: string = ''
}

export const FileType: Constant[] = [
    {
        key: '1',
        value: '文件'
    },
    {
        key: '2',
        value: '文件目录'
    }
]
