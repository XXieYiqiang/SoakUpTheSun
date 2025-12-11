package dir

import (
	"errors"
	"os"
)

// PathExists 判断路径是否存在
func PathExists(path string) (bool, error) {
	fi, err := os.Stat(path)
	if err == nil {
		if fi.IsDir() {
			return true, nil
		}
		return false, errors.New("路径存在,但不是目录")
	}
	if os.IsNotExist(err) {
		return false, nil
	}
	return false, err
}
