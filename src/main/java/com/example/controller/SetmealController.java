package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.CustomException;
import com.example.common.R;
import com.example.dto.SetmealDto;
import com.example.entity.Category;
import com.example.entity.Setmeal;
import com.example.entity.SetmealDish;
import com.example.service.CategoryService;
import com.example.service.SetmealService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {

        String key = "setmeal_" + setmeal.getCategoryId() + "_" + setmeal.getStatus();
        List<Setmeal> setmeals = (List<Setmeal>) redisTemplate.opsForValue().get(key);
        if (setmeals != null) {
            return R.success(setmeals);
        }
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();
        queryWrapper.eq(Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.eq(Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.orderByAsc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        redisTemplate.opsForValue().set(key, list, 60, TimeUnit.MINUTES);
        return R.success(list);
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        Page<Setmeal> pageInfo = new Page<Setmeal>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<SetmealDto>();
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();
        queryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, queryWrapper);

        BeanUtils.copyProperties(pageInfo, dtoPage, "records");

        List<SetmealDto> records = pageInfo.getRecords().stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            setmealDto.setCategoryName(categoryService.getById(item.getCategoryId()).getName());
            return setmealDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(records);

        return R.success(dtoPage);
    }

    @PostMapping()
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveWithDish(setmealDto);
        String key = "setmeal_" + setmealDto.getCategoryId() + "*";
        redisTemplate.delete(key);
        return R.success("保存成功");
    }

    @GetMapping("/{id}")
    public R<SetmealDto> getByIdWithDish(@PathVariable Long id) {
        return R.success(setmealService.getByIdWithDish(id));
    }

    @PutMapping
    public R<String> updateWithDish(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithDish(setmealDto);
        String key = "setmeal_" + setmealDto.getCategoryId() + "*";
        redisTemplate.delete(key);
        return R.success("修改成功");
    }

    @DeleteMapping()
    public R<String> remove(@RequestParam("ids") List<Long> ids) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);
        if (setmealService.count(queryWrapper) > 0) {
            throw new CustomException("当前套餐正在出售，不能删除！");
        }

        setmealService.removeByIdsWithDish(ids);
        return R.success("删除成功");
    }

    @PostMapping("/status/{status}")
    public R<String> update(@PathVariable int status, @RequestParam("ids") List<Long> ids) {
        for(Long id : ids) {
            Setmeal setmeal = new Setmeal();
            setmeal.setId(id);
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }
        String key = "setmeal*";
        redisTemplate.delete(key);
        return R.success("更新成功");
    }

}
